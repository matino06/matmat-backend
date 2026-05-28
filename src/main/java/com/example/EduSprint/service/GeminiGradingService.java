package com.example.EduSprint.service;

import com.example.EduSprint.entity.MockExamQuestion;
import com.example.EduSprint.entity.MockExamScoringCriterion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class GeminiGradingService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient;
    private final String apiKey;
    private final String modelText;
    private final String modelVision;

    public GeminiGradingService(@Value("${gemini.api-key}") String apiKey,
                                @Value("${gemini.model-text}") String modelText,
                                @Value("${gemini.model-vision}") String modelVision,
                                @Value("${gemini.api-base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.modelText = modelText;
        this.modelVision = modelVision;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    public AiGradeResult gradeShortAnswer(MockExamQuestion question, String userAnswer, String parentQuestionText) {
        String prompt = buildShortAnswerPrompt(question, userAnswer, parentQuestionText);
        ObjectNode body = baseRequestBody(prompt, null, null);
        body.set("generationConfig", scalarGradeSchemaConfig(question.getPoints()));
        return parseScalar(callGemini(body, modelText));
    }

    public AiGradeResult gradeImageAnswer(MockExamQuestion question, byte[] imageBytes, String mimeType, String parentQuestionText) {
        String prompt = buildImageAnswerPrompt(question, parentQuestionText);
        ObjectNode body = baseRequestBody(prompt, imageBytes, mimeType);
        body.set("generationConfig", scalarGradeSchemaConfig(question.getPoints()));
        return parseScalar(callGemini(body, modelVision));
    }

    public ExtendedAiGradeResult gradeExtendedAnswer(MockExamQuestion question,
                                                     List<MockExamScoringCriterion> criteria,
                                                     byte[] imageBytes,
                                                     String mimeType,
                                                     String parentQuestionText) {
        String prompt = buildExtendedAnswerPrompt(question, criteria, parentQuestionText);
        ObjectNode body = baseRequestBody(prompt, imageBytes, mimeType);
        body.set("generationConfig", extendedGradeSchemaConfig());
        return parseExtended(callGemini(body, modelVision), criteria);
    }

    private ObjectNode baseRequestBody(String prompt, byte[] imageBytes, String mimeType) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", prompt);
        if (imageBytes != null) {
            ObjectNode inlinePart = parts.addObject();
            ObjectNode inline = inlinePart.putObject("inline_data");
            inline.put("mime_type", mimeType != null ? mimeType : "image/png");
            inline.put("data", Base64.getEncoder().encodeToString(imageBytes));
        }
        return root;
    }

    private ObjectNode scalarGradeSchemaConfig(Short maxPoints) {
        ObjectNode config = objectMapper.createObjectNode();
        config.put("responseMimeType", "application/json");
        ObjectNode schema = config.putObject("responseSchema");
        schema.put("type", "OBJECT");
        ObjectNode props = schema.putObject("properties");
        ObjectNode score = props.putObject("score");
        score.put("type", "INTEGER");
        score.put("description", "Bodovi od 0 do " + maxPoints);
        ObjectNode feedback = props.putObject("feedback");
        feedback.put("type", "STRING");
        ObjectNode isCorrect = props.putObject("isCorrect");
        isCorrect.put("type", "BOOLEAN");
        ArrayNode required = schema.putArray("required");
        required.add("score");
        required.add("feedback");
        required.add("isCorrect");
        return config;
    }

    private ObjectNode extendedGradeSchemaConfig() {
        ObjectNode config = objectMapper.createObjectNode();
        config.put("responseMimeType", "application/json");
        ObjectNode schema = config.putObject("responseSchema");
        schema.put("type", "OBJECT");
        ObjectNode props = schema.putObject("properties");

        ObjectNode criterionScores = props.putObject("criterionScores");
        criterionScores.put("type", "ARRAY");
        ObjectNode item = criterionScores.putObject("items");
        item.put("type", "OBJECT");
        ObjectNode itemProps = item.putObject("properties");
        itemProps.putObject("criterionId").put("type", "INTEGER");
        itemProps.putObject("points").put("type", "INTEGER");
        itemProps.putObject("feedback").put("type", "STRING");
        ArrayNode itemReq = item.putArray("required");
        itemReq.add("criterionId");
        itemReq.add("points");
        itemReq.add("feedback");

        ObjectNode overall = props.putObject("overallFeedback");
        overall.put("type", "STRING");

        ArrayNode required = schema.putArray("required");
        required.add("criterionScores");
        required.add("overallFeedback");
        return config;
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

    private JsonNode callGemini(ObjectNode body, String model) {
        String path = "/models/" + model + ":generateContent";
        String response = webClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).queryParam("key", apiKey).build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body.toString())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (text.isMissingNode() || text.isNull()) {
                throw new RuntimeException("Gemini response missing text part: " + response);
            }
            return objectMapper.readTree(text.asText());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
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
