package com.example.EduSprint.service;

import com.example.EduSprint.config.AppTime;
import com.example.EduSprint.dto.BuildJob;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.Collectors;

// Izvršava poslove izrade vlastitih predmeta. Ima vlastiti bazen dretvi, odvojen od AI ocjenjivanja
// probne mature, i radi najviše own-courses.build.concurrency koraka istodobno na ovoj instanci.
@Component
public class BuildJobRunner {

    private static final Logger log = LoggerFactory.getLogger(BuildJobRunner.class);

    private final BuildJobQueue queue;
    private final BuildQuotaService quota;
    private final Map<String, BuildStep> steps;
    private final NamedParameterJdbcTemplate jdbc;
    private final Semaphore capacity;
    private final ExecutorService executor;
    private final String resendApiKey;
    private LocalDate lastLimitAlert;

    // Koraci se registriraju kao beanovi; dok nijedan ne postoji, izvršitelj ne radi ništa.
    @Autowired
    public BuildJobRunner(BuildJobQueue queue, BuildQuotaService quota, ObjectProvider<BuildStep> steps,
                          NamedParameterJdbcTemplate jdbc,
                          @Value("${own-courses.build.concurrency:8}") int concurrency,
                          @Value("${resend.api-key}") String resendApiKey,
                          @Value("${own-courses.build.enabled:true}") boolean enabled) {
        this(queue, quota, enabled ? steps.orderedStream().toList() : List.of(), jdbc, concurrency, resendApiKey);
    }

    BuildJobRunner(BuildJobQueue queue, BuildQuotaService quota, List<BuildStep> steps,
                   NamedParameterJdbcTemplate jdbc, int concurrency, String resendApiKey) {
        this.queue = queue;
        this.quota = quota;
        this.steps = steps.stream().collect(Collectors.toMap(BuildStep::kind, Function.identity()));
        this.jdbc = jdbc;
        this.capacity = new Semaphore(concurrency);
        this.executor = Executors.newFixedThreadPool(concurrency, r -> {
            Thread t = new Thread(r, "own-course-build");
            t.setDaemon(true);
            return t;
        });
        this.resendApiKey = resendApiKey;
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 10000)
    public void tick() {
        if (steps.isEmpty()) return; // izvršitelj je isključen (own-courses.build.enabled=false) ili nema koraka
        try {
            queue.recoverExpiredLeases();
            while (capacity.tryAcquire()) {
                BuildJob job = queue.claimNext();
                if (job == null) {
                    capacity.release();
                    return;
                }
                if (job.countsQuota() && !quota.hasQuota(job.accountId())) {
                    queue.markWaitingQuota(job);
                    capacity.release();
                    continue;
                }
                executor.execute(() -> {
                    try {
                        run(job);
                    } finally {
                        capacity.release();
                    }
                });
            }
        } catch (RuntimeException e) {
            log.error("Red poslova izrade: {}", e.getMessage(), e);
        }
    }

    void run(BuildJob job) {
        BuildStep step = steps.get(job.kind());
        if (step == null) {
            queue.fail(job, "Nepoznata vrsta koraka: " + job.kind());
            return;
        }
        try {
            queue.complete(job, step.run(job));
            step.afterComplete(job);
        } catch (OwnCourseLlm.GlobalLimitException e) {
            queue.markPaused(job);
            alertGlobalLimit(e.getMessage());
        } catch (Exception e) {
            log.warn("Korak {} posla {} nije uspio (pokušaj {}): {}", job.kind(), job.jobId(), job.attempts(), e.getMessage());
            queue.fail(job, e.getMessage());
        }
    }

    // Kad se kvota obnovi (1. u mjesecu) ili joj admin poveća limit, poslovi koji su čekali nastavljaju sami.
    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void resumeWaitingQuota() {
        for (Long accountId : queue.accountsWaitingForQuota()) {
            if (quota.hasQuota(accountId)) queue.resumeWaitingQuota(accountId);
        }
    }

    // Pauzirani poslovi (globalni limit) pokušavaju ponovno svakih pola sata; ako je limit još potrošen,
    // odmah se ponovno pauziraju bez trošenja pokušaja.
    @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 30 * 60 * 1000)
    public void resumePaused() {
        queue.resumePaused();
    }

    // Admin dobije najviše jedan e-mail dnevno po instanci.
    private synchronized void alertGlobalLimit(String detail) {
        LocalDate today = AppTime.today();
        if (today.equals(lastLimitAlert)) return;
        lastLimitAlert = today;
        try {
            List<String> admins = jdbc.queryForList("SELECT email FROM account WHERE is_admin", new MapSqlParameterSource(), String.class);
            if (admins.isEmpty()) return;
            new Resend(resendApiKey).emails().send(CreateEmailOptions.builder()
                    .from("MatMat <info@matmat.online>")
                    .to(admins)
                    .subject("Vlastiti predmeti: potrošen je limit OpenRouter ključa")
                    .html("Izrada vlastitih predmeta je pauzirana jer je potrošen mjesečni limit ključa "
                            + "<code>OPENROUTER_OWN_COURSES_API_KEY</code> (ili na računu nema kredita). "
                            + "Poslovi se nastavljaju sami kad se limit poveća.<br><br><small>" + HtmlUtils.htmlEscape(String.valueOf(detail)) + "</small>")
                    .build());
        } catch (Exception e) {
            log.error("E-mail o globalnom limitu nije poslan: {}", e.getMessage());
        }
    }
}
