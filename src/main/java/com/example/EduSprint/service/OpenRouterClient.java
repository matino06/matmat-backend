package com.example.EduSprint.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Locale;
import java.util.Set;

/**
 * Tanki klijent za OpenRouter Chat Completions API (OpenAI-kompatibilan).
 * Instance (s različitim API ključevima) se kreiraju u OpenRouterConfig.
 */
public class OpenRouterClient {

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE =
            new ParameterizedTypeReference<>() {};

    /** Vrijednosti koje OpenRouter prihvaća za reasoning.effort. */
    static final Set<String> REASONING_EFFORTS = Set.of("xhigh", "high", "medium", "low", "minimal", "none");

    /**
     * Razina razmišljanja iz postavki: prazno → null (model razmišlja po svom zadanom),
     * inače jedna od REASONING_EFFORTS. Krivu vrijednost odbija pri spremanju postavki umjesto
     * da je OpenRouter tiho ignorira.
     */
    public static String reasoningEffort(String value) {
        if (value == null || value.isBlank()) return null;
        String effort = value.strip().toLowerCase(Locale.ROOT);
        if (!REASONING_EFFORTS.contains(effort)) {
            throw new IllegalArgumentException(
                    "Nepoznat reasoning effort '" + value + "', dopušteno: " + REASONING_EFFORTS);
        }
        return effort;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient;
    private final String baseUrl;

    public OpenRouterClient(String apiKey, String baseUrl) {
        this.baseUrl = baseUrl;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader("HTTP-Referer", "https://matmat.online")
                .defaultHeader("X-Title", "MatMat")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    /**
     * Every call goes only to providers that do not collect or train on the data ({@code data_collection: "deny"}),
     * whatever the caller set. Checked 2026-10-15: every model in use (chat, grading text and vision, own-course
     * writer, embeddings, decisions) still has such a provider.
     */
    static ObjectNode denyDataCollection(ObjectNode body) {
        ObjectNode provider = body.has("provider") && body.get("provider").isObject()
                ? (ObjectNode) body.get("provider") : body.putObject("provider");
        provider.put("data_collection", "deny");
        return body;
    }

    /** Blokirajući poziv; vraća cijeli JSON odgovor. */
    public JsonNode complete(ObjectNode body) {
        String response = request(body)
                .bodyToMono(String.class)
                .block();
        try {
            return objectMapper.readTree(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }

    /** POST na drugi OpenRouter endpoint; path je relativan na baseUrl (npr. „/embeddings“) ili apsolutan URL. */
    public JsonNode post(String path, ObjectNode body) {
        return parse(webClient.post().uri(uri(path)).contentType(MediaType.APPLICATION_JSON).bodyValue(denyDataCollection(body).toString())
                .retrieve().onStatus(HttpStatusCode::isError, this::error).bodyToMono(String.class).block());
    }

    public JsonNode get(String path) {
        return parse(webClient.get().uri(uri(path))
                .retrieve().onStatus(HttpStatusCode::isError, this::error).bodyToMono(String.class).block());
    }

    /** Apsolutni URL za endpoint izvan /api/v1 (npr. „alpha/decisions“ → https://openrouter.ai/api/alpha/decisions). */
    public String apiUrl(String pathUnderApi) {
        return baseUrl.replaceFirst("/v1/?$", "/") + pathUnderApi;
    }

    private java.net.URI uri(String path) {
        return java.net.URI.create(path.startsWith("http") ? path : baseUrl + path);
    }

    private reactor.core.publisher.Mono<? extends Throwable> error(org.springframework.web.reactive.function.client.ClientResponse r) {
        return r.createException().map(ex -> new RuntimeException("OpenRouter " + ex.getStatusCode().value() + ": "
                + ex.getResponseBodyAsString(), ex));
    }

    private JsonNode parse(String response) {
        try {
            return objectMapper.readTree(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenRouter response: " + e.getMessage(), e);
        }
    }

    /** Streaming poziv (body mora imati "stream": true); emitira svaki JSON chunk. */
    public Flux<JsonNode> stream(ObjectNode body) {
        return request(body)
                .bodyToFlux(SSE_TYPE)
                // keep-alive komentari (": OPENROUTER PROCESSING") nemaju data
                .mapNotNull(ServerSentEvent::data)
                .filter(data -> data != null && !data.isBlank() && !"[DONE]".equals(data.strip()))
                .map(this::parseChunk);
    }

    private WebClient.ResponseSpec request(ObjectNode body) {
        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(denyDataCollection(body).toString())
                .retrieve()
                .onStatus(HttpStatusCode::isError, r -> r.createException()
                        .map(ex -> new RuntimeException("OpenRouter " + ex.getStatusCode().value() + ": "
                                + ex.getResponseBodyAsString(), ex)));
    }

    private JsonNode parseChunk(String data) {
        try {
            return objectMapper.readTree(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI stream chunk: " + e.getMessage(), e);
        }
    }
}
