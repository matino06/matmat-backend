package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

// Pozivi OpenRoutera za izradu vlastitih predmeta, preko zasebnog ključa s mjesečnim limitom.
// Svaki poziv sprema stvarni trošak (usage.cost) uz posao, pa se iz njega računa kvota kreatora.
//
// Prompt caching: zajednički velik dio prompta ide u system poruku, a dio koji se mijenja u user poruku.
// OpenAI modeli keširaju zajednički početak sami; Anthropic modelima treba oznaka cache_control.
@Service
public class OwnCourseLlm {

    public record Options(String reasoning, int maxTokens, boolean json) {
        public static Options of(String reasoning, int maxTokens) {
            return new Options(reasoning, maxTokens, true);
        }
    }

    public record Result(String content, BigDecimal costUsd, int cachedTokens) {
    }

    // Globalni mjesečni limit ključa za vlastite predmete je potrošen (ili na računu nema kredita).
    public static class GlobalLimitException extends RuntimeException {
        public GlobalLimitException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final OpenRouterClient client;
    private final BuildJobQueue queue;
    private final ObjectMapper mapper = new ObjectMapper();

    public OwnCourseLlm(@Qualifier("ownCoursesOpenRouterClient") OpenRouterClient client, BuildJobQueue queue) {
        this.client = client;
        this.queue = queue;
    }

    public Result chat(BuildJob job, String model, String system, String user, Options options) {
        JsonNode response;
        try {
            response = client.complete(body(model, system, user, options));
        } catch (RuntimeException e) {
            if (isGlobalLimit(e.getMessage())) throw new GlobalLimitException(e.getMessage(), e);
            throw e;
        }
        JsonNode usage = response.path("usage");
        BigDecimal cost = usage.hasNonNull("cost") ? usage.get("cost").decimalValue() : BigDecimal.ZERO;
        queue.recordCost(job, model, cost);
        String content = response.path("choices").path(0).path("message").path("content").asText("");
        int cached = usage.path("prompt_tokens_details").path("cached_tokens").asInt(0);
        return new Result(content, cost, cached);
    }

    ObjectNode body(String model, String system, String user, Options options) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", options.maxTokens());
        body.putObject("usage").put("include", true);
        if (options.json()) body.putObject("response_format").put("type", "json_object");
        if (options.reasoning() != null) body.putObject("reasoning").put("effort", options.reasoning());
        ArrayNode messages = body.putArray("messages");
        ObjectNode sys = messages.addObject().put("role", "system");
        if (model.startsWith("anthropic/")) {
            ObjectNode part = sys.putArray("content").addObject();
            part.put("type", "text").put("text", system);
            part.putObject("cache_control").put("type", "ephemeral");
        } else {
            sys.put("content", system);
        }
        messages.addObject().put("role", "user").put("content", user);
        return body;
    }

    // OpenRouter vraća 403 kad je potrošen limit ključa, a 402 kad na računu nema kredita.
    static boolean isGlobalLimit(String message) {
        if (message == null) return false;
        return message.startsWith("OpenRouter 402")
                || (message.startsWith("OpenRouter 403") && message.toLowerCase().contains("limit"));
    }
}
