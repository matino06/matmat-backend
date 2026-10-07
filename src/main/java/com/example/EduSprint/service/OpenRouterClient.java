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

/**
 * Tanki klijent za OpenRouter Chat Completions API (OpenAI-kompatibilan).
 * Instance (s različitim API ključevima) se kreiraju u OpenRouterConfig.
 */
public class OpenRouterClient {

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient;

    public OpenRouterClient(String apiKey, String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader("HTTP-Referer", "https://matmat.online")
                .defaultHeader("X-Title", "MatMat")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
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
                .bodyValue(body.toString())
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
