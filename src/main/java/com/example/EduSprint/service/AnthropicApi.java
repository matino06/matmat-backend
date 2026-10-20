package com.example.EduSprint.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.helpers.MessageAccumulator;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.anthropic.models.messages.ThinkingConfigBetweenTools;
import com.anthropic.models.messages.ThinkingConfigDisabled;
import com.anthropic.models.messages.Usage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Anthropic API (kredit uz Claude Max) za izradu vlastitih predmeta, AI chat i ocjenjivanje mature. Dok na kreditu
// ima više od credit-min, poziv Claude modela ide ravno na Anthropic, inače (ili kad poziv ne uspije) na OpenRouter.
//
// Anthropic API ne vraća ni stanje kredita ni cijenu poziva. Cijena se zato računa iz tokena i sprema u anthropic_cost,
// a ostatak je početno stanje (iz Consolea) minus sva tamo zapisana potrošnja. Ključ koristi samo backend.
@Service
public class AnthropicApi {

    private static final Logger log = LoggerFactory.getLogger(AnthropicApi.class);

    public static final String SOURCE_OWN_COURSE = "own_course";
    public static final String SOURCE_CHAT = "chat";
    public static final String SOURCE_GRADING = "grading";

    // $ za milijun tokena: ulaz, izlaz, čitanje iz cachea; zapis u cache (5 min) stoji 1,25 × ulaz.
    // Model koji ovdje nema cijenu ide na OpenRouter, jer mu se potrošnja ne bi mogla oduzeti od kredita. Popis ima
    // samo modele koji primaju adaptive thinking i sve razine effort (starijima treba budget_tokens ili nemaju xhigh).
    record Price(BigDecimal input, BigDecimal output, BigDecimal cacheRead) {
        Price(String input, String output, String cacheRead) {
            this(new BigDecimal(input), new BigDecimal(output), new BigDecimal(cacheRead));
        }

        BigDecimal cacheWrite() {
            return input.multiply(new BigDecimal("1.25"));
        }
    }

    static final Map<String, Price> PRICES = Map.ofEntries(
            Map.entry("claude-fable-5-1", new Price("10.00", "50.00", "0.25")),
            Map.entry("claude-fable-5", new Price("10.00", "50.00", "1.00")),
            Map.entry("claude-opus-5-5", new Price("4.00", "20.00", "0.20")),
            Map.entry("claude-opus-5", new Price("5.00", "25.00", "0.50")),
            Map.entry("claude-opus-4-8", new Price("5.00", "25.00", "0.50")),
            Map.entry("claude-opus-4-7", new Price("5.00", "25.00", "0.50")),
            Map.entry("claude-sonnet-5-5", new Price("2.00", "10.00", "0.20")),
            Map.entry("claude-sonnet-5", new Price("2.00", "10.00", "0.20")),
            // Do 100K tokena prompta; dulji promptovi (0,50 / 2,50 $) se u chatu i ocjenjivanju ne šalju.
            Map.entry("claude-haiku-5-5", new Price("0.10", "0.50", "0.01")));

    private static final Pattern DATA_URI = Pattern.compile("^data:(image/[A-Za-z0-9.+-]+);base64,(.+)$", Pattern.DOTALL);

    private final AnthropicClient client; // null kad ključ nije postavljen
    private final NamedParameterJdbcTemplate jdbc;
    private final BigDecimal creditStart;
    private final BigDecimal creditMin;
    private final ObjectMapper mapper = new ObjectMapper();
    // Razlog zbog kojeg se Anthropic više ne koristi do ponovnog pokretanja (nema kredita, neispravan ključ).
    private volatile String off;

    public AnthropicApi(NamedParameterJdbcTemplate jdbc,
                        @Value("${anthropic.api-key:}") String apiKey,
                        @Value("${anthropic.credit-start:0}") BigDecimal creditStart,
                        @Value("${anthropic.credit-min:2}") BigDecimal creditMin) {
        this.jdbc = jdbc;
        this.client = apiKey == null || apiKey.isBlank() ? null : AnthropicOkHttpClient.builder().apiKey(apiKey.strip()).build();
        this.creditStart = creditStart;
        this.creditMin = creditMin;
    }

    AnthropicClient client() {
        return client;
    }

    // Ima li smisla poslati poziv na Anthropic: ključ postoji, nije isključen i ostatak kredita je iznad minimuma.
    public boolean available() {
        if (client == null || off != null) return false;
        try {
            return remaining().compareTo(creditMin) > 0;
        } catch (DataAccessException e) {
            // Bez tablice potrošnje kredit se ne može pratiti, pa sve ide na OpenRouter.
            log.warn("Anthropic kredit se ne može izračunati, poziv ide na OpenRouter: {}", e.getMessage());
            return false;
        }
    }

    public BigDecimal remaining() {
        BigDecimal spent = jdbc.queryForObject("SELECT COALESCE(SUM(cost_usd), 0) FROM anthropic_cost",
                new MapSqlParameterSource(), BigDecimal.class);
        return creditStart.subtract(spent == null ? BigDecimal.ZERO : spent);
    }

    public void recordCost(String source, String model, BigDecimal cost) {
        try {
            jdbc.update("INSERT INTO anthropic_cost (source, model, cost_usd) VALUES (:s, :m, :c)",
                    new MapSqlParameterSource("s", source).addValue("m", model).addValue("c", cost));
        } catch (DataAccessException e) {
            log.error("Anthropic: trošak {} $ ({}, {}) nije zapisan: {}", cost, source, model, e.getMessage());
        }
    }

    // Anthropic ID modela za OpenRouter ID iz postavki („anthropic/claude-haiku-5.5“ → „claude-haiku-5-5“),
    // ako se taj poziv sad može poslati ravno na Anthropic.
    public Optional<String> route(String openRouterModel) {
        String model = apiModel(openRouterModel);
        return model != null && available() ? Optional.of(model) : Optional.empty();
    }

    static String apiModel(String openRouterModel) {
        if (openRouterModel == null || !openRouterModel.startsWith("anthropic/")) return null;
        String id = openRouterModel.substring("anthropic/".length());
        int variant = id.indexOf(':'); // OpenRouter varijante (:nitro, :floor…) su isti model
        if (variant >= 0) id = id.substring(0, variant);
        id = id.replace('.', '-');
        return PRICES.containsKey(id) ? id : null;
    }

    // Poziv nije uspio: kod potrošenog kredita ili neispravnog ključa Anthropic se isključuje, inače vrijedi
    // samo za taj poziv. U oba slučaja isti poziv ide na OpenRouter.
    void failed(RuntimeException e) {
        if (e instanceof AnthropicServiceException se) {
            String reason = disablingReason(se);
            if (reason != null) {
                off = reason;
                log.warn("Anthropic isključen do ponovnog pokretanja ({}), pozivi idu na OpenRouter: {}", reason, e.getMessage());
            } else {
                log.warn("Anthropic poziv nije uspio ({}), ovaj poziv ide na OpenRouter: {}", se.statusCode(), e.getMessage());
            }
        } else {
            log.warn("Anthropic nedostupan, ovaj poziv ide na OpenRouter: {}", e.getMessage());
        }
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

    static BigDecimal cost(String model, Usage usage) {
        return cost(model, usage.inputTokens(), usage.outputTokens(),
                usage.cacheCreationInputTokens().orElse(0L), usage.cacheReadInputTokens().orElse(0L));
    }

    static BigDecimal cost(String model, long input, long output, long cacheWrite, long cacheRead) {
        Price p = PRICES.get(model);
        return p.input().multiply(BigDecimal.valueOf(input))
                .add(p.output().multiply(BigDecimal.valueOf(output)))
                .add(p.cacheWrite().multiply(BigDecimal.valueOf(cacheWrite)))
                .add(p.cacheRead().multiply(BigDecimal.valueOf(cacheRead)))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }

    // ---------------------------------------------------------------- chat i ocjenjivanje

    // Isti poziv kao OpenRouterClient.complete; za Claude model dok ima kredita ide na Anthropic, a odgovor se vraća
    // u OpenRouter obliku (choices[0].message.content, usage), pa pozivatelj ne razlikuje put.
    public JsonNode complete(ObjectNode body, String source, OpenRouterClient fallback) {
        Optional<String> model = route(body.path("model").asText(null));
        if (model.isEmpty()) return fallback.complete(body);
        MessageCreateParams params = params(model.get(), body);
        Message message;
        // Streaming: s velikim max_tokens poziv bez streama prekida HTTP timeout.
        try (StreamResponse<RawMessageStreamEvent> stream = client.messages().createStreaming(params)) {
            MessageAccumulator accumulator = MessageAccumulator.create();
            stream.stream().forEach(accumulator::accumulate);
            message = accumulator.message();
        } catch (AnthropicServiceException | AnthropicIoException e) {
            failed(e);
            return fallback.complete(body);
        }
        BigDecimal cost = cost(model.get(), message.usage());
        recordCost(source, model.get(), cost);
        if (message.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
            log.warn("{} je odbio poziv ({}), ovaj poziv ide na OpenRouter", model.get(), source);
            return fallback.complete(body);
        }
        StringBuilder text = new StringBuilder();
        message.content().forEach(block -> block.text().ifPresent(t -> text.append(t.text())));
        ObjectNode root = mapper.createObjectNode().put("model", model.get()).put("provider", "Anthropic API");
        ObjectNode choice = root.putArray("choices").addObject();
        choice.putObject("message").put("role", "assistant").put("content", text.toString());
        choice.put("finish_reason", finishReason(message.stopReason().orElse(null)));
        usage(root, model.get(), message.usage().inputTokens() + message.usage().cacheCreationInputTokens().orElse(0L)
                + message.usage().cacheReadInputTokens().orElse(0L), message.usage().outputTokens(), cost);
        return root;
    }

    // Isti poziv kao OpenRouterClient.stream; chunkovi su u OpenRouter obliku (delta.content, finish_reason, usage s
    // cijenom). Ako Anthropic padne ili odbije prije prve riječi odgovora, cijeli poziv ide na OpenRouter.
    public Flux<JsonNode> stream(ObjectNode body, String source, OpenRouterClient fallback) {
        return Flux.defer(() -> {
            Optional<String> model = route(body.path("model").asText(null));
            if (model.isEmpty()) return fallback.stream(body);
            AtomicBoolean answered = new AtomicBoolean(false);
            return anthropicStream(model.get(), params(model.get(), body), source, answered)
                    .onErrorResume(e -> !answered.get() && e instanceof Retry, e -> fallback.stream(body))
                    .onErrorResume(e -> !answered.get() && (e instanceof AnthropicServiceException || e instanceof AnthropicIoException),
                            e -> {
                                failed((RuntimeException) e);
                                return fallback.stream(body);
                            });
        });
    }

    // Odbijanje prije ijedne riječi: isti poziv ide na OpenRouter.
    private static final class Retry extends RuntimeException {
        Retry() {
            super(null, null, false, false);
        }
    }

    private Flux<JsonNode> anthropicStream(String model, MessageCreateParams params, String source, AtomicBoolean answered) {
        long[] tokens = new long[4]; // ulaz, izlaz, zapis u cache, čitanje iz cachea
        AtomicReference<StopReason> stop = new AtomicReference<>();
        AtomicBoolean recorded = new AtomicBoolean(false);
        // Trošak se zapisuje jednom, i kad učenik zaustavi odgovor: tokeni do tada su plaćeni.
        Supplier<BigDecimal> record = () -> {
            BigDecimal cost = cost(model, tokens[0], tokens[1], tokens[2], tokens[3]);
            if (recorded.compareAndSet(false, true) && tokens[0] + tokens[1] + tokens[2] + tokens[3] > 0) {
                recordCost(source, model, cost);
            }
            return cost;
        };
        return Flux.<JsonNode, StreamResponse<RawMessageStreamEvent>>using(
                        () -> client.messages().createStreaming(params),
                        response -> Flux.fromStream(response.stream()).concatMap(event -> {
                            List<JsonNode> out = new ArrayList<>(1);
                            event.messageStart().ifPresent(start -> {
                                Usage u = start.message().usage();
                                tokens[0] = u.inputTokens();
                                tokens[1] = u.outputTokens();
                                tokens[2] = u.cacheCreationInputTokens().orElse(0L);
                                tokens[3] = u.cacheReadInputTokens().orElse(0L);
                            });
                            event.contentBlockDelta().flatMap(d -> d.delta().text()).ifPresent(t -> {
                                if (t.text().isEmpty()) return;
                                answered.set(true);
                                ObjectNode chunk = mapper.createObjectNode().put("model", model);
                                chunk.putArray("choices").addObject().putObject("delta").put("content", t.text());
                                out.add(chunk);
                            });
                            event.messageDelta().ifPresent(delta -> {
                                // Brojevi u message_delta su ukupni do tog trenutka, ne prirast.
                                tokens[1] = delta.usage().outputTokens();
                                delta.usage().inputTokens().ifPresent(v -> tokens[0] = v);
                                delta.usage().cacheCreationInputTokens().ifPresent(v -> tokens[2] = v);
                                delta.usage().cacheReadInputTokens().ifPresent(v -> tokens[3] = v);
                                delta.delta().stopReason().ifPresent(stop::set);
                            });
                            if (event.messageStop().isPresent()) {
                                BigDecimal cost = record.get();
                                if (StopReason.REFUSAL.equals(stop.get())) {
                                    log.warn("{} je odbio poziv ({})", model, source);
                                    if (!answered.get()) return Flux.error(new Retry());
                                }
                                ObjectNode last = mapper.createObjectNode().put("model", model);
                                last.putArray("choices").addObject().putObject("delta");
                                ((ObjectNode) last.path("choices").path(0)).put("finish_reason", finishReason(stop.get()));
                                usage(last, model, tokens[0] + tokens[2] + tokens[3], tokens[1], cost);
                                out.add(last);
                            }
                            return Flux.fromIterable(out);
                        }),
                        StreamResponse::close)
                .doFinally(signal -> record.get())
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static String finishReason(StopReason stop) {
        if (StopReason.MAX_TOKENS.equals(stop)) return "length";
        if (StopReason.REFUSAL.equals(stop)) return "content_filter";
        return "stop";
    }

    private static void usage(ObjectNode root, String model, long promptTokens, long completionTokens, BigDecimal cost) {
        root.putObject("usage")
                .put("prompt_tokens", promptTokens)
                .put("completion_tokens", completionTokens)
                .put("cost", cost);
    }

    // OpenRouter tijelo poziva (Chat Completions) → Anthropic Messages. Pokriva ono što šalju chat i ocjenjivanje:
    // system i poruke kao tekst ili dijelovi (text s cache_control, image_url kao data: URI), max_tokens,
    // reasoning.effort i response_format json_schema.
    MessageCreateParams params(String model, JsonNode body) {
        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(body.path("max_tokens").asLong(16_000));
        List<TextBlockParam> system = new ArrayList<>();
        for (JsonNode message : body.path("messages")) {
            String role = message.path("role").asText();
            List<ContentBlockParam> blocks = blocks(message.path("content"));
            if ("system".equals(role)) {
                blocks.forEach(b -> b.text().ifPresent(system::add));
            } else if (!blocks.isEmpty()) {
                params.addMessage(MessageParam.builder()
                        .role("assistant".equals(role) ? MessageParam.Role.ASSISTANT : MessageParam.Role.USER)
                        .contentOfBlockParams(blocks)
                        .build());
            }
        }
        if (!system.isEmpty()) params.systemOfTextBlockParams(system);

        OutputConfig.Builder output = OutputConfig.builder();
        boolean hasOutput = false;
        String effort = body.path("reasoning").path("effort").asText(null);
        if (effort != null) {
            Thinking thinking = thinking(model, effort);
            if (thinking.effort() != null) {
                output.effort(OutputConfig.Effort.of(thinking.effort()));
                hasOutput = true;
            }
            switch (thinking.mode()) {
                case ADAPTIVE -> params.thinking(ThinkingConfigAdaptive.builder().build());
                case BETWEEN_TOOLS -> params.thinking(ThinkingConfigBetweenTools.builder().build());
                case DISABLED -> params.thinking(ThinkingConfigDisabled.builder().build());
                case DEFAULT -> { }
            }
        }
        JsonNode schema = body.path("response_format").path("json_schema").path("schema");
        if (schema.isObject()) {
            JsonOutputFormat.Schema.Builder s = JsonOutputFormat.Schema.builder();
            schema.fields().forEachRemaining(e -> s.putAdditionalProperty(e.getKey(), JsonValue.fromJsonNode(e.getValue())));
            output.format(JsonOutputFormat.builder().schema(s.build()).build());
            hasOutput = true;
        }
        if (hasOutput) params.outputConfig(output.build());
        return params.build();
    }

    private List<ContentBlockParam> blocks(JsonNode content) {
        List<ContentBlockParam> blocks = new ArrayList<>();
        if (content.isTextual()) {
            if (!content.asText().isBlank()) blocks.add(ContentBlockParam.ofText(TextBlockParam.builder().text(content.asText()).build()));
            return blocks;
        }
        for (JsonNode part : content) {
            boolean cached = part.has("cache_control");
            switch (part.path("type").asText()) {
                case "text" -> {
                    if (part.path("text").asText().isEmpty()) continue;
                    TextBlockParam.Builder text = TextBlockParam.builder().text(part.path("text").asText());
                    if (cached) text.cacheControl(CacheControlEphemeral.builder().build());
                    blocks.add(ContentBlockParam.ofText(text.build()));
                }
                case "image_url" -> {
                    Matcher m = DATA_URI.matcher(part.path("image_url").path("url").asText(""));
                    if (!m.matches()) continue; // chat i ocjenjivanje šalju samo data: URI-je
                    blocks.add(ContentBlockParam.ofImage(ImageBlockParam.builder()
                            .source(Base64ImageSource.builder()
                                    .mediaType(Base64ImageSource.MediaType.of(m.group(1)))
                                    .data(m.group(2))
                                    .build())
                            .build()));
                }
                default -> { }
            }
        }
        return blocks;
    }

    enum ThinkingMode { DEFAULT, ADAPTIVE, BETWEEN_TOOLS, DISABLED }

    record Thinking(ThinkingMode mode, String effort) {
    }

    // reasoning.effort iz postavki (xhigh…minimal, none) → thinking i effort za Anthropic. „none“ znači bez
    // razmišljanja: Sonnet 5.5 ga gasi s between_tools, a Opus 5.5 i Fable se ne mogu ugasiti, pa razmišljaju što manje.
    static Thinking thinking(String model, String effort) {
        return switch (effort) {
            case "none" -> {
                if (model.equals("claude-sonnet-5-5")) yield new Thinking(ThinkingMode.BETWEEN_TOOLS, null);
                if (model.equals("claude-opus-5-5") || model.startsWith("claude-fable-")) yield new Thinking(ThinkingMode.DEFAULT, "low");
                yield new Thinking(ThinkingMode.DISABLED, null);
            }
            case "minimal" -> new Thinking(ThinkingMode.ADAPTIVE, "low");
            default -> new Thinking(ThinkingMode.ADAPTIVE, effort);
        };
    }
}
