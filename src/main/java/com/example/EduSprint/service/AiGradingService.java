package com.example.EduSprint.service;

import com.example.EduSprint.dto.AiSettingsDTO;
import com.example.EduSprint.entity.MockExamQuestion;
import com.example.EduSprint.entity.MockExamScoringCriterion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiGradingService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenRouterClient openRouterClient;
    private static final Logger log = LoggerFactory.getLogger(AiGradingService.class);
    public static final String UNREADABLE_IMAGE_MARKER = "SLIKA_NECITLJIVA";
    private static final String LATEX_RULE =
            "Matematičke izraze u feedbacku piši isključivo kao inline LaTeX unutar $...$ (npr. $[0, 2\\pi]$). " +
            "Ne koristi \\(, \\[ ni $$. Svaki otvoreni $ mora biti zatvoren.";
    private static final String UNREADABLE_IMAGE_RULE =
            "Ako je slika prazna, potpuno crna, mutna ili nečitljiva, NE ocjenjuj sadržaj — postavi score 0 " +
            "i u feedback napiši točno '" + UNREADABLE_IMAGE_MARKER + "'.";

    private final AiSettingsService aiSettingsService;

    public AiGradingService(@Qualifier("gradingOpenRouterClient") OpenRouterClient openRouterClient,
                            AiSettingsService aiSettingsService) {
        this.openRouterClient = openRouterClient;
        this.aiSettingsService = aiSettingsService;
    }

    public AiGradeResult gradeShortAnswer(MockExamQuestion question, String userAnswer, String parentQuestionText) {
        String prompt = buildShortAnswerPrompt(question, userAnswer, parentQuestionText);
        ObjectNode body = baseRequestBody(false, prompt, null);
        body.set("response_format", jsonSchemaFormat("grade", scalarGradeSchema(question.getPoints())));
        return parseScalar(callModel(body));
    }

    public AiGradeResult gradeImageAnswer(MockExamQuestion question, byte[] imageBytes, String parentQuestionText) {
        String prompt = buildImageAnswerPrompt(question, parentQuestionText);
        ObjectNode body = baseRequestBody(true, prompt, imageBytes);
        body.set("response_format", jsonSchemaFormat("grade", scalarGradeSchema(question.getPoints())));
        return parseScalar(callModel(body));
    }

    public ExtendedAiGradeResult gradeExtendedAnswer(MockExamQuestion question,
                                                     List<MockExamScoringCriterion> criteria,
                                                     byte[] imageBytes,
                                                     String parentQuestionText) {
        String prompt = buildExtendedAnswerPrompt(question, criteria, parentQuestionText);
        ObjectNode body = baseRequestBody(true, prompt, imageBytes);
        body.set("response_format", jsonSchemaFormat("extended_grade", extendedGradeSchema()));
        return parseExtended(callModel(body), criteria);
    }

    private ObjectNode baseRequestBody(boolean vision, String prompt, byte[] imageBytes) {
        AiSettingsDTO settings = aiSettingsService.current();
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", vision ? settings.gradingModelVision() : settings.gradingModelText());
        root.put("max_tokens", settings.gradingMaxTokens());
        if (settings.gradingReasoningEffort() != null) {
            root.putObject("reasoning").put("effort", settings.gradingReasoningEffort());
        }
        ArrayNode messages = root.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        ArrayNode content = message.putArray("content");
        content.addObject().put("type", "text").put("text", prompt);
        if (imageBytes != null) {
            ObjectNode imagePart = content.addObject();
            imagePart.put("type", "image_url");
            imagePart.putObject("image_url").put("url", ImageNormalizer.toJpegDataUri(imageBytes));
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
        sb.append("Vrati JSON: score (0..").append(q.getPoints()).append("), feedback (kratko obrazloženje na hrvatskom), isCorrect (true ako je matematička vrijednost točna). ");
        sb.append(LATEX_RULE);
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
        sb.append(UNREADABLE_IMAGE_RULE).append(" ");
        sb.append("Vrati JSON: score (0..").append(q.getPoints()).append("), feedback (kratko obrazloženje na hrvatskom), isCorrect. ");
        sb.append(LATEX_RULE);
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
        sb.append("overallFeedback (sažetak za učenika s objašnjenjem oduzimanja bodova ako je primijenjeno, na hrvatskom). ");
        sb.append("Ako je slika prazna, potpuno crna, mutna ili nečitljiva, NE ocjenjuj sadržaj — svim kriterijima daj 0 bodova " +
                "i u overallFeedback napiši točno '" + UNREADABLE_IMAGE_MARKER + "'. ");
        sb.append(LATEX_RULE);
        return sb.toString();
    }

    private JsonNode callModel(ObjectNode body) {
        JsonNode root = openRouterClient.complete(body);
        log.info("AI grading response: model={}, provider={}",
                root.path("model").asText(""), root.path("provider").asText(""));
        JsonNode text = root.path("choices").path(0).path("message").path("content");
        if (text.isMissingNode() || text.isNull()) {
            throw new RuntimeException("AI response missing message content: " + root);
        }
        try {
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
