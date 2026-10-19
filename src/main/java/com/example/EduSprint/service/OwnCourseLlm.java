package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Optional;

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
    private final OwnCourseAnthropic anthropic;
    private final ObjectMapper mapper = new ObjectMapper();

    public OwnCourseLlm(@Qualifier("ownCoursesOpenRouterClient") OpenRouterClient client, BuildJobQueue queue,
                        OwnCourseAnthropic anthropic) {
        this.client = client;
        this.queue = queue;
        this.anthropic = anthropic;
    }

    // Dok na Anthropic kreditu ima dovoljno, poziv Sola ide na Sonnet; ako on ne uspije, ide na OpenRouter.
    public Result chat(BuildJob job, String model, String system, String user, Options options) {
        if (anthropic.replaces(model)) {
            Optional<Result> result = anthropic.chat(system, user, options, cost -> queue.recordCost(job, anthropic.model(), cost));
            if (result.isPresent()) return result.get();
        }
        return call(job, model, body(model, system, mapper.getNodeFactory().textNode(user), options));
    }

    // Poziv sa slikom (stranica materijala): tekst + slika kao data URL.
    public Result chatWithImage(BuildJob job, String model, String system, String user, byte[] image,
                                String mimeType, Options options) {
        ArrayNode content = mapper.createArrayNode();
        content.addObject().put("type", "text").put("text", user);
        content.addObject().put("type", "image_url").putObject("image_url")
                .put("url", "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(image));
        return call(job, model, body(model, system, content, options));
    }

    private Result call(BuildJob job, String model, ObjectNode body) {
        JsonNode response;
        try {
            response = client.complete(body);
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
        return body(model, system, mapper.getNodeFactory().textNode(user), options);
    }

    ObjectNode body(String model, String system, JsonNode userContent, Options options) {
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
        messages.addObject().put("role", "user").set("content", userContent);
        return body;
    }

    // Embeddingi tekstova (za predfilter „preblizu zadatku s ispita“), istim redom kao ulaz.
    public java.util.List<double[]> embed(BuildJob job, String model, java.util.List<String> texts) {
        ObjectNode body = mapper.createObjectNode().put("model", model);
        ArrayNode input = body.putArray("input");
        texts.forEach(input::add);
        JsonNode response = guarded(() -> client.post("/embeddings", body));
        record(job, model, response);
        java.util.List<double[]> out = new java.util.ArrayList<>();
        for (JsonNode d : response.path("data")) {
            double[] v = new double[d.path("embedding").size()];
            for (int i = 0; i < v.length; i++) v[i] = d.path("embedding").get(i).asDouble();
            out.add(v);
        }
        if (out.size() != texts.size()) throw new IllegalStateException("Embeddings: " + out.size() + " od " + texts.size());
        return out;
    }

    // Model odluke (OpenRouter decisions): vjerojatnost (noul) da je odgovor na pitanje „da“.
    public double decide(BuildJob job, String model, ObjectNode state, String question, String instructions) {
        ObjectNode body = mapper.createObjectNode().put("model", model);
        body.set("state", state);
        body.putObject("questions").putObject(question).put("type", "noul").put("instructions", instructions);
        JsonNode response = guarded(() -> client.post(client.apiUrl("alpha/decisions"), body));
        record(job, model, response);
        JsonNode noul = response.path("answers").path(question).path("noul");
        if (!noul.isNumber()) throw new IllegalStateException("Odluka bez noul: " + response);
        return noul.asDouble();
    }

    // Batch (pola cijene, do 24 h): zahtjevi su tijela chat poziva bez modela; vraća id batcha.
    public String createBatch(String model, java.util.Map<String, ObjectNode> requests) {
        ObjectNode body = mapper.createObjectNode();
        body.put("endpoint", "/v1/chat/completions");
        body.put("model", model);
        body.put("completion_window", "24h");
        ArrayNode list = body.putArray("requests"); // „requests“ mora biti zadnje polje
        requests.forEach((id, request) -> {
            request.remove("model");
            list.addObject().put("custom_id", id).set("body", request);
        });
        return guarded(() -> client.post("/batches", body)).path("id").asText();
    }

    public JsonNode getBatch(String batchId) {
        return guarded(() -> client.get("/batches/" + batchId));
    }

    private JsonNode guarded(java.util.function.Supplier<JsonNode> call) {
        try {
            return call.get();
        } catch (RuntimeException e) {
            if (isGlobalLimit(e.getMessage())) throw new GlobalLimitException(e.getMessage(), e);
            throw e;
        }
    }

    private void record(BuildJob job, String model, JsonNode response) {
        JsonNode cost = response.path("usage").path("cost");
        queue.recordCost(job, model, cost.isNumber() ? cost.decimalValue() : BigDecimal.ZERO);
    }

    // OpenRouter vraća 403 kad je potrošen limit ključa, a 402 kad na računu nema kredita.
    static boolean isGlobalLimit(String message) {
        if (message == null) return false;
        return message.startsWith("OpenRouter 402")
                || (message.startsWith("OpenRouter 403") && message.toLowerCase().contains("limit"));
    }
}
