package com.example.EduSprint.service;

import com.example.EduSprint.dto.AiSettingsDTO;
import com.example.EduSprint.dto.AiSettingsUpdateDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.AiSettings;
import com.example.EduSprint.repository.AiSettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

/**
 * AI postavke (modeli, dnevni limit, max tokens, reasoning effort) iz tablice ai_settings.
 * Čitaju se pri svakom AI pozivu, pa se drže u kratkom cacheu; admin promjena vrijedi odmah,
 * a ručna izmjena u bazi najkasnije nakon CACHE_TTL.
 */
@Service
public class AiSettingsService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    private static final int MAX_MODEL_CHARS = 200;

    private final AiSettingsRepository aiSettingsRepository;
    private final TransactionTemplate transactionTemplate;

    private volatile Cached cached;

    public AiSettingsService(AiSettingsRepository aiSettingsRepository, TransactionTemplate transactionTemplate) {
        this.aiSettingsRepository = aiSettingsRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public AiSettingsDTO current() {
        Cached c = cached;
        if (c == null || c.loadedAt().plus(CACHE_TTL).isBefore(Instant.now())) {
            c = reload();
        }
        return c.settings();
    }

    public AiSettingsDTO update(Account admin, AiSettingsUpdateDTO req) {
        AiSettingsUpdateDTO valid = validate(req);
        transactionTemplate.executeWithoutResult(tx -> {
            AiSettings settings = aiSettingsRepository.findById(AiSettings.ID)
                    .orElseThrow(() -> new IllegalStateException("Tablica ai_settings nema red s id = 1"));
            settings.setChatModel(valid.chatModel());
            settings.setChatDailyLimit(valid.chatDailyLimit());
            settings.setChatMaxTokens(valid.chatMaxTokens());
            settings.setChatReasoningEffort(valid.chatReasoningEffort());
            settings.setGradingModelText(valid.gradingModelText());
            settings.setGradingModelVision(valid.gradingModelVision());
            settings.setGradingMaxTokens(valid.gradingMaxTokens());
            settings.setGradingReasoningEffort(valid.gradingReasoningEffort());
            settings.setUpdatedAt(Instant.now());
            settings.setUpdatedBy(admin);
        });
        // Tek nakon commita, da drugi zahtjev ne učita i zadrži staru vrijednost.
        return reload().settings();
    }

    private Cached reload() {
        AiSettingsDTO settings = aiSettingsRepository.findCurrent()
                .orElseThrow(() -> new IllegalStateException("Tablica ai_settings nema red s id = 1"));
        Cached c = new Cached(settings, Instant.now());
        cached = c;
        return c;
    }

    /** Provjeri i normaliziraj admin unos; greške idu kao 400. */
    static AiSettingsUpdateDTO validate(AiSettingsUpdateDTO req) {
        if (req == null) {
            throw badRequest("Nedostaju postavke");
        }
        return new AiSettingsUpdateDTO(
                model("chatModel", req.chatModel()),
                atLeast("chatDailyLimit", req.chatDailyLimit(), 0),
                atLeast("chatMaxTokens", req.chatMaxTokens(), 1),
                effort("chatReasoningEffort", req.chatReasoningEffort()),
                model("gradingModelText", req.gradingModelText()),
                model("gradingModelVision", req.gradingModelVision()),
                atLeast("gradingMaxTokens", req.gradingMaxTokens(), 1),
                effort("gradingReasoningEffort", req.gradingReasoningEffort()));
    }

    private static String model(String field, String value) {
        String model = value == null ? "" : value.strip();
        if (model.isEmpty() || model.length() > MAX_MODEL_CHARS || model.chars().anyMatch(Character::isWhitespace)) {
            throw badRequest(field + " mora biti OpenRouter ID modela (npr. anthropic/claude-haiku-5.5)");
        }
        return model;
    }

    private static Integer atLeast(String field, Integer value, int min) {
        if (value == null || value < min) {
            throw badRequest(field + " mora biti cijeli broj >= " + min);
        }
        return value;
    }

    private static String effort(String field, String value) {
        try {
            return OpenRouterClient.reasoningEffort(value);
        } catch (IllegalArgumentException e) {
            throw badRequest(field + ": " + e.getMessage());
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record Cached(AiSettingsDTO settings, Instant loadedAt) {
    }
}
