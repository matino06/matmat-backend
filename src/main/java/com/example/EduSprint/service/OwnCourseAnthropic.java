package com.example.EduSprint.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.helpers.MessageAccumulator;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.anthropic.models.messages.Usage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

// Izrada vlastitih predmeta preko Anthropic API-ja (kredit uz Claude Max): dok na kreditu ima više od credit-min,
// pozivi modela koji se zamjenjuje (Sol) idu na Sonnet. Kad poziv ne uspije, isti poziv ide na OpenRouter.
//
// Anthropic API ne vraća ni stanje kredita ni cijenu poziva. Cijena se zato računa iz tokena i sprema u build_cost,
// a ostatak je početno stanje (iz Consolea) minus sva tamo zapisana potrošnja ovog modela. Ključ koristi samo backend.
@Service
public class OwnCourseAnthropic {

    private static final Logger log = LoggerFactory.getLogger(OwnCourseAnthropic.class);

    // Cijene Sonneta 5.5 u $ za milijun tokena; zapis u cache (5 min) stoji 1,25 × ulaz, čitanje 0,1 × ulaz.
    static final BigDecimal PRICE_INPUT = new BigDecimal("2.00");
    static final BigDecimal PRICE_OUTPUT = new BigDecimal("10.00");
    static final BigDecimal PRICE_CACHE_WRITE = new BigDecimal("2.50");
    static final BigDecimal PRICE_CACHE_READ = new BigDecimal("0.20");

    // Razmišljanje se broji u max_tokens; limiti koraka su pisani za Sol (od 1000 naviše) i na high bi ga presjekli.
    // Plaćaju se samo stvarno potrošeni tokeni, pa veći limit ne poskupljuje poziv.
    static final long MIN_MAX_TOKENS = 64_000;

    private final AnthropicClient client; // null kad ključ nije postavljen
    private final NamedParameterJdbcTemplate jdbc;
    private final String model;
    private final String effort;
    private final String replaces;
    private final BigDecimal creditStart;
    private final BigDecimal creditMin;
    // Razlog zbog kojeg se Anthropic više ne koristi do ponovnog pokretanja (nema kredita, neispravan ključ).
    private volatile String off;

    public OwnCourseAnthropic(NamedParameterJdbcTemplate jdbc,
                              @Value("${own-courses.anthropic.api-key:}") String apiKey,
                              @Value("${own-courses.anthropic.model:claude-sonnet-5-5}") String model,
                              @Value("${own-courses.anthropic.effort:high}") String effort,
                              @Value("${own-courses.anthropic.replaces:openai/gpt-6.1-sol}") String replaces,
                              @Value("${own-courses.anthropic.credit-start:0}") BigDecimal creditStart,
                              @Value("${own-courses.anthropic.credit-min:2}") BigDecimal creditMin) {
        this.jdbc = jdbc;
        this.client = apiKey == null || apiKey.isBlank() ? null : AnthropicOkHttpClient.builder().apiKey(apiKey.strip()).build();
        this.model = model;
        this.effort = effort;
        this.replaces = replaces;
        this.creditStart = creditStart;
        this.creditMin = creditMin;
    }

    public String model() {
        return model;
    }

    // Ima li smisla poslati poziv na Anthropic: ključ postoji, nije isključen i ostatak kredita je iznad minimuma.
    public boolean available() {
        return client != null && off == null && remaining().compareTo(creditMin) > 0;
    }

    public boolean replaces(String openRouterModel) {
        return replaces.equals(openRouterModel) && available();
    }

    public BigDecimal remaining() {
        BigDecimal spent = jdbc.queryForObject("SELECT COALESCE(SUM(cost_usd), 0) FROM build_cost WHERE model = :m",
                new MapSqlParameterSource("m", model), BigDecimal.class);
        return creditStart.subtract(spent == null ? BigDecimal.ZERO : spent);
    }

    // Poziv Sonneta; prazno znači da isti poziv treba poslati na OpenRouter. Trošak se predaje i kad odgovor
    // nije upotrebljiv (odbijanje), jer je plaćen.
    public Optional<OwnCourseLlm.Result> chat(String system, String user, OwnCourseLlm.Options options,
                                              Consumer<BigDecimal> recordCost) {
        String instructions = options.json() ? system + "\n\nOdgovori samo jednim JSON objektom, bez teksta prije ili poslije." : system;
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(Math.max(options.maxTokens(), MIN_MAX_TOKENS))
                .thinking(ThinkingConfigAdaptive.builder().build())
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.of(effort)).build())
                .systemOfTextBlockParams(List.of(TextBlockParam.builder().text(instructions)
                        .cacheControl(CacheControlEphemeral.builder().build()).build()))
                .addUserMessage(user)
                .build();
        Message message;
        // Streaming: s velikim max_tokens poziv bez streama prekida HTTP timeout.
        try (StreamResponse<RawMessageStreamEvent> stream = client.messages().createStreaming(params)) {
            MessageAccumulator accumulator = MessageAccumulator.create();
            stream.stream().forEach(accumulator::accumulate);
            message = accumulator.message();
        } catch (AnthropicServiceException e) {
            String reason = disablingReason(e);
            if (reason != null) {
                off = reason;
                log.warn("Anthropic isključen do ponovnog pokretanja ({}), izrada ide na OpenRouter: {}", reason, e.getMessage());
            } else {
                log.warn("Anthropic poziv nije uspio ({}), ovaj poziv ide na OpenRouter: {}", e.statusCode(), e.getMessage());
            }
            return Optional.empty();
        } catch (AnthropicIoException e) {
            log.warn("Anthropic nedostupan, ovaj poziv ide na OpenRouter: {}", e.getMessage());
            return Optional.empty();
        }
        BigDecimal cost = cost(message.usage());
        recordCost.accept(cost);
        if (message.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
            log.warn("Sonnet je odbio poziv, ovaj poziv ide na OpenRouter");
            return Optional.empty();
        }
        StringBuilder text = new StringBuilder();
        message.content().forEach(block -> block.text().ifPresent(t -> text.append(t.text())));
        int cached = message.usage().cacheReadInputTokens().orElse(0L).intValue();
        return Optional.of(new OwnCourseLlm.Result(text.toString(), cost, cached));
    }

    // Kredit je potrošen (402 billing_error, a starije poruke 400 „credit balance is too low“) ili ključ ne vrijedi:
    // svi daljnji pozivi bi pali isto, pa se Anthropic isključuje. Ostale greške vrijede samo za taj poziv.
    static String disablingReason(AnthropicServiceException e) {
        if (e.statusCode() == 402) return "nema kredita";
        if (e instanceof BadRequestException && e.getMessage() != null
                && e.getMessage().toLowerCase().contains("credit balance")) return "nema kredita";
        if (e instanceof UnauthorizedException || e instanceof PermissionDeniedException) return "ključ ne vrijedi";
        return null;
    }

    static BigDecimal cost(Usage usage) {
        return cost(usage.inputTokens(), usage.outputTokens(),
                usage.cacheCreationInputTokens().orElse(0L), usage.cacheReadInputTokens().orElse(0L));
    }

    static BigDecimal cost(long input, long output, long cacheWrite, long cacheRead) {
        return PRICE_INPUT.multiply(BigDecimal.valueOf(input))
                .add(PRICE_OUTPUT.multiply(BigDecimal.valueOf(output)))
                .add(PRICE_CACHE_WRITE.multiply(BigDecimal.valueOf(cacheWrite)))
                .add(PRICE_CACHE_READ.multiply(BigDecimal.valueOf(cacheRead)))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }
}
