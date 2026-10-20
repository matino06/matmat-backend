package com.example.EduSprint.service;

import com.anthropic.core.http.StreamResponse;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.helpers.MessageAccumulator;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

// Izrada vlastitih predmeta preko Anthropic API-ja (AnthropicApi): dok na kreditu ima više od credit-min,
// pozivi modela koji se zamjenjuje (Sol) idu na Sonnet. Kad poziv ne uspije, isti poziv ide na OpenRouter.
// Trošak se zapisuje i u build_cost (kvota kreatora) i u anthropic_cost (kredit).
@Service
public class OwnCourseAnthropic {

    private static final Logger log = LoggerFactory.getLogger(OwnCourseAnthropic.class);

    // Razmišljanje se broji u max_tokens; limiti koraka su pisani za Sol (od 1000 naviše) i na high bi ga presjekli.
    // Plaćaju se samo stvarno potrošeni tokeni, pa veći limit ne poskupljuje poziv.
    static final long MIN_MAX_TOKENS = 64_000;

    private final AnthropicApi api;
    private final String model;
    private final String effort;
    private final String replaces;

    public OwnCourseAnthropic(AnthropicApi api,
                              @Value("${own-courses.anthropic.model:claude-sonnet-5-5}") String model,
                              @Value("${own-courses.anthropic.effort:high}") String effort,
                              @Value("${own-courses.anthropic.replaces:openai/gpt-6.1-sol}") String replaces) {
        this.api = api;
        this.model = model;
        this.effort = effort;
        this.replaces = replaces;
    }

    public String model() {
        return model;
    }

    public boolean available() {
        return api.available();
    }

    public boolean replaces(String openRouterModel) {
        return replaces.equals(openRouterModel) && available();
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
        try (StreamResponse<RawMessageStreamEvent> stream = api.client().messages().createStreaming(params)) {
            MessageAccumulator accumulator = MessageAccumulator.create();
            stream.stream().forEach(accumulator::accumulate);
            message = accumulator.message();
        } catch (AnthropicServiceException | AnthropicIoException e) {
            api.failed(e);
            return Optional.empty();
        }
        BigDecimal cost = AnthropicApi.cost(model, message.usage());
        api.recordCost(AnthropicApi.SOURCE_OWN_COURSE, model, cost);
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
}
