package com.example.EduSprint.service;

import com.example.EduSprint.entity.MockExamQuestion;
import com.example.EduSprint.entity.MockExamScoringCriterion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class AiGradingService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient;
    private final String modelText;
    private final String modelVision;

    public AiGradingService(@Value("${openrouter.api-key}") String apiKey,
                            @Value("${openrouter.model-text}") String modelText,
                            @Value("${openrouter.model-vision}") String modelVision,
                            @Value("${openrouter.api-base-url}") String baseUrl) {
        this.modelText = modelText;
        this.modelVision = modelVision;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader("HTTP-Referer", "https://matmat.online")
                .defaultHeader("X-Title", "MatMat")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    public AiGradeResult gradeShortAnswer(MockExamQuestion question, String userAnswer, String parentQuestionText) {
        String prompt = buildShortAnswerPrompt(question, userAnswer, parentQuestionText);
        ObjectNode body = baseRequestBody(modelText, prompt, null, null);
        body.set("response_format", jsonSchemaFormat("grade", scalarGradeSchema(question.getPoints())));
        return parseScalar(callModel(body));
    }

    public AiGradeResult gradeImageAnswer(MockExamQuestion question, byte[] imageBytes, String mimeType, String parentQuestionText) {
        String prompt = buildImageAnswerPrompt(question, parentQuestionText);
        ObjectNode body = baseRequestBody(modelVision, prompt, imageBytes, mimeType);
        body.set("response_format", jsonSchemaFormat("grade", scalarGradeSchema(question.getPoints())));
        return parseScalar(callModel(body));
    }

    public ExtendedAiGradeResult gradeExtendedAnswer(MockExamQuestion question,
                                                     List<MockExamScoringCriterion> criteria,
                                                     byte[] imageBytes,
                                                     String mimeType,
                                                     String parentQuestionText) {
        String prompt = buildExtendedAnswerPrompt(question, criteria, parentQuestionText);
        ObjectNode body = baseRequestBody(modelVision, prompt, imageBytes, mimeType);
        body.set("response_format", jsonSchemaFormat("extended_grade", extendedGradeSchema()));
        return parseExtended(callModel(body), criteria);
    }

    private ObjectNode baseRequestBody(String model, String prompt, byte[] imageBytes, String mimeType) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        ArrayNode messages = root.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        ArrayNode content = message.putArray("content");
        content.addObject().put("type", "text").put("text", prompt);
        if (imageBytes != null) {
            String mime = mimeType != null ? mimeType : "image/png";
            ObjectNode imagePart = content.addObject();
            imagePart.put("type", "image_url");
            imagePart.putObject("image_url")
                    .put("url", "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(imageBytes));
        }
        return root;
    }

    private ObjectNode jsonSchemaFormat(String name, ObjectNode schema) {
        ObjectNode format = objectMapper.createObjectNode();
        format.put("type", "json_schema");
        ObjectNode jsonSchema = format.putObject("json_schema");
        jsonSchema.put("name", name);
        jsonSchema.put("strict", true);
        jsonSchema.set("schema", schema);
        return format;
    }

    private ObjectNode scalarGradeSchema(Short maxPoints) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        ObjectNode score = props.putObject("score");
        score.put("type", "integer");
        score.put("description", "Bodovi od 0 do " + maxPoints);
        props.putObject("feedback").put("type", "string");
        props.putObject("isCorrect").put("type", "boolean");
        ArrayNode required = schema.putArray("required");
        required.add("score");
        required.add("feedback");
        required.add("isCorrect");
        schema.put("additionalProperties", false);
        return schema;
    }

    private ObjectNode extendedGradeSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");

        ObjectNode criterionScores = props.putObject("criterionScores");
        criterionScores.put("type", "array");
        ObjectNode item = criterionScores.putObject("items");
        item.put("type", "object");
        ObjectNode itemProps = item.putObject("properties");
        itemProps.putObject("criterionId").put("type", "integer");
        itemProps.putObject("points").put("type", "integer");
        itemProps.putObject("feedback").put("type", "string");
        ArrayNode itemReq = item.putArray("required");
        itemReq.add("criterionId");
        itemReq.add("points");
        itemReq.add("feedback");
        item.put("additionalProperties", false);

        props.putObject("overallFeedback").put("type", "string");

        ArrayNode required = schema.putArray("required");
        required.add("criterionScores");
        required.add("overallFeedback");
        schema.put("additionalProperties", false);
        return schema;
    }

    private String buildShortAnswerPrompt(MockExamQuestion q, String userAnswer, String parentQuestionText) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ti si ocjenjivač državne mature iz matematike. Ocijeni odgovor učenika striktno prema točnom odgovoru.\n\n");
        if (parentQuestionText != null && !parentQuestionText.isBlank()) {
            sb.append("Kontekst zadatka (uvodni tekst uz pitanje):\n").append(parentQuestionText).append("\n\n");
        }
        sb.append("Pitanje:\n").append(nullSafe(q.getQuestionText())).append("\n\n");
        sb.append("Točan odgovor: ").append(nullSafe(q.getCorrectAnswer())).append("\n");
        if (q.getAnswerNotes() != null && !q.getAnswerNotes().isBlank()) {
            sb.append("Napomene za ocjenjivanje (prihvatljivi ekvivalentni zapisi): ").append(q.getAnswerNotes()).append("\n");
        }
        sb.append("Maksimalan broj bodova: ").append(q.getPoints()).append("\n\n");
        sb.append("Odgovor učenika: ").append(nullSafe(userAnswer)).append("\n\n");
        sb.append("VAŽNO — PRAVILO FORMATA: Ocjenjuješ isključivo matematičku točnost, NE format zapisa. ");
        sb.append("Ako je matematička vrijednost odgovora točna, daj pun broj bodova bez obzira na format ");
        sb.append("(npr. '1/2', '0,5', '0.5', '50%', LaTeX zapis — sve su ekvivalentne). ");
        sb.append("Samo ako uočiš problem s formatom, možeš ga kratko napomenuti u feedbacku, ali to NE smije utjecati na score. ");
        sb.append("Vrati JSON: score (0..").append(q.getPoints()).append("), feedback (kratko obrazloženje na hrvatskom), isCorrect (true ako je matematička vrijednost točna).");
        return sb.toString();
    }

    private String buildImageAnswerPrompt(MockExamQuestion q, String parentQuestionText) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ti si ocjenjivač državne mature iz matematike. Učenik je predao odgovor kao sliku (rukopis, graf ili crtež).\n\n");
        if (parentQuestionText != null && !parentQuestionText.isBlank()) {
            sb.append("Kontekst zadatka (uvodni tekst uz pitanje):\n").append(parentQuestionText).append("\n\n");
        }
        sb.append("Pitanje:\n").append(nullSafe(q.getQuestionText())).append("\n\n");
        sb.append("Točan odgovor / opis ispravnog rješenja: ").append(nullSafe(q.getCorrectAnswer())).append("\n");
        if (q.getAnswerNotes() != null && !q.getAnswerNotes().isBlank()) {
            sb.append("Napomene za ocjenjivanje: ").append(q.getAnswerNotes()).append("\n");
        }
        sb.append("Maksimalan broj bodova: ").append(q.getPoints()).append("\n\n");
        sb.append("Pažljivo pročitaj sliku, prepoznaj što je učenik nacrtao/napisao i ocijeni striktno prema točnom odgovoru. ");
        sb.append("Vrati JSON: score (0..").append(q.getPoints()).append("), feedback (kratko obrazloženje na hrvatskom), isCorrect.");
        return sb.toString();
    }

    private static final String EXTENDED_GRADING_RULES =
            "OBVEZNA PRAVILA OCJENJIVANJA (primjenjuju se na sve zadatke s postupkom):\n" +
            "1. Priznaju se točna rješenja dobivena različitim načinima.\n" +
            "2. MORA biti prikazan postupak rješavanja — samo konačan odgovor bez postupka ne donosi bodove.\n" +
            "3. Pristupniku koji je pogrešno prepisao zadatak, te ga zatim točno riješio " +
            "(a da pritom zadatak nije promijenio smisao niti je pojednostavljen) " +
            "oduzima se 1 bod od predviđenoga broja bodova za taj zadatak.\n" +
            "4. Pristupnik koji je učinio grešku, a da pritom zadatak nije promijenio smisao " +
            "niti je pojednostavljen, boduju se svi ispravno provedeni koraci (SLIJEDI GREŠKU — " +
            "tj. nastavi ocjenjivati daljnji postupak kao da je međurezultat točan).\n" +
            "5. Pristupnik NE MOŽE dobiti maksimalan broj bodova ukoliko nema točno konačno rješenje.\n" +
            "6. Pristupnik NE MOŽE dobiti maksimalan broj bodova ukoliko ima točno rješenje uz " +
            "matematički nepotpun ili netočan postupak.\n";

    private String buildExtendedAnswerPrompt(MockExamQuestion q, List<MockExamScoringCriterion> criteria, String parentQuestionText) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ti si ocjenjivač državne mature iz matematike. Učenik je predao postupak rješavanja kao sliku.\n\n");
        sb.append(EXTENDED_GRADING_RULES).append("\n");
        if (parentQuestionText != null && !parentQuestionText.isBlank()) {
            sb.append("Kontekst zadatka (uvodni tekst uz pitanje):\n").append(parentQuestionText).append("\n\n");
        }
        sb.append("Pitanje:\n").append(nullSafe(q.getQuestionText())).append("\n\n");
        if (q.getCorrectAnswer() != null && !q.getCorrectAnswer().isBlank()) {
            sb.append("Točan konačan odgovor: ").append(q.getCorrectAnswer()).append("\n");
        }
        if (q.getAnswerNotes() != null && !q.getAnswerNotes().isBlank()) {
            sb.append("Napomene za ocjenjivanje: ").append(q.getAnswerNotes()).append("\n");
        }
        sb.append("\nKriteriji ocjenjivanja (svaki bod se dodjeljuje neovisno):\n");
        for (MockExamScoringCriterion c : criteria) {
            sb.append("- criterionId=").append(c.getCriterionId())
              .append(", maxPoints=").append(c.getPoints())
              .append(": ").append(c.getDescription()).append("\n");
        }
        sb.append("\nZa svaki kriterij dodijeli bodove 0..maxPoints i kratko obrazloženje primijenjenih pravila. ");
        sb.append("Vrati JSON s nizom criterionScores (svaki s criterionId, points, feedback) i ");
        sb.append("overallFeedback (sažetak za učenika s objašnjenjem oduzimanja bodova ako je primijenjeno, na hrvatskom).");
        return sb.toString();
    }

    private JsonNode callModel(ObjectNode body) {
        String response = webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body.toString())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode text = root.path("choices").path(0).path("message").path("content");
            if (text.isMissingNode() || text.isNull()) {
                throw new RuntimeException("AI response missing message content: " + response);
            }
            return objectMapper.readTree(stripCodeFence(text.asText()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }

    private static String stripCodeFence(String s) {
        String t = s.strip();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            int lastFence = t.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                t = t.substring(firstNewline + 1, lastFence).strip();
            }
        }
        return t;
    }

    private AiGradeResult parseScalar(JsonNode node) {
        int score = node.path("score").asInt(0);
        String feedback = node.path("feedback").asText("");
        boolean isCorrect = node.path("isCorrect").asBoolean(false);
        return new AiGradeResult((short) score, feedback, isCorrect);
    }

    private ExtendedAiGradeResult parseExtended(JsonNode node, List<MockExamScoringCriterion> criteria) {
        List<CriterionGrade> grades = new ArrayList<>();
        JsonNode arr = node.path("criterionScores");
        if (arr.isArray()) {
            for (JsonNode item : arr) {
                long criterionId = item.path("criterionId").asLong(-1);
                int points = item.path("points").asInt(0);
                String feedback = item.path("feedback").asText("");
                Short maxPoints = criteria.stream()
                        .filter(c -> c.getCriterionId() != null && c.getCriterionId() == criterionId)
                        .map(MockExamScoringCriterion::getPoints)
                        .findFirst()
                        .orElse((short) 0);
                int clamped = Math.max(0, Math.min(points, maxPoints));
                grades.add(new CriterionGrade(criterionId, (short) clamped, feedback));
            }
        }
        String overall = node.path("overallFeedback").asText("");
        return new ExtendedAiGradeResult(grades, overall);
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    public record AiGradeResult(Short score, String feedback, boolean isCorrect) {
    }

    public record CriterionGrade(long criterionId, Short pointsAwarded, String feedback) {
    }

    public record ExtendedAiGradeResult(List<CriterionGrade> criterionScores, String overallFeedback) {
    }
}
