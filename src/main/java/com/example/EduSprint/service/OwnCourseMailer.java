package com.example.EduSprint.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

// E-mailovi kreatoru o izradi vlastitog predmeta. Neuspjelo slanje se samo zapiše; izrada ne smije pasti zbog maila.
@Component
public class OwnCourseMailer {

    private static final Logger log = LoggerFactory.getLogger(OwnCourseMailer.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final String resendApiKey;

    public OwnCourseMailer(NamedParameterJdbcTemplate jdbc, @Value("${resend.api-key:}") String resendApiKey) {
        this.jdbc = jdbc;
        this.resendApiKey = resendApiKey;
    }

    public void planReady(long courseId) {
        send(courseId, "Plan za „%s“ je spreman za pregled",
                "Plan lekcija za tvoj predmet <b>%s</b> je izrađen. Pregledaj ga, promijeni što želiš i potvrdi, "
                        + "pa krećemo s izradom zadataka.<br><br><a href=\"https://matmat.online/moji-predmeti/%d\">Otvori plan</a>");
    }

    public void tasksReady(long courseId) {
        send(courseId, "Zadaci za „%s“ su spremni",
                "Svi zadaci za tvoj predmet <b>%s</b> su izrađeni. Možeš učiti cijeli plan.<br><br>"
                        + "<a href=\"https://matmat.online/moji-predmeti/%d\">Otvori predmet</a>");
    }

    private void send(long courseId, String subject, String html) {
        try {
            var row = jdbc.queryForMap("""
                    SELECT c.course_name, a.email FROM course c JOIN account a ON a.account_id = c.owner_account_id
                    WHERE c.course_id = :c""", new MapSqlParameterSource("c", courseId));
            String name = (String) row.get("course_name");
            if (resendApiKey == null || resendApiKey.isBlank()) return; // lokalno bez Resend ključa
            new Resend(resendApiKey).emails().send(CreateEmailOptions.builder()
                    .from("MatMat <info@matmat.online>")
                    .to((String) row.get("email"))
                    .subject(subject.formatted(name))
                    .html(html.formatted(HtmlUtils.htmlEscape(name), courseId))
                    .build());
        } catch (Exception e) {
            log.warn("E-mail o predmetu {} nije poslan: {}", courseId, e.getMessage());
        }
    }
}
