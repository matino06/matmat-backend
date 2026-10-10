package com.example.EduSprint.service;

import com.example.EduSprint.dto.AiChatRequestDTO;
import com.example.EduSprint.dto.AiSettingsDTO;
import com.example.EduSprint.dto.AiUsageDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.repository.AiConversationRepository;
import com.example.EduSprint.repository.AiMessageRepository;
import com.example.EduSprint.repository.AiPromptRepository;
import com.example.EduSprint.repository.ExplanationStepRepository;
import com.example.EduSprint.repository.TaskRepository;
import com.example.EduSprint.storage.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Razgovori bez zadatka (ispit / općenito): limit se vodi u memoriji, pa se može testirati bez baze.
class AiChatRateLimitTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private OpenRouterClient openRouter;
    private AiChatService service;
    private Account account;

    @BeforeEach
    void setUp() {
        openRouter = mock(OpenRouterClient.class);
        AiSettingsService settings = mock(AiSettingsService.class);
        when(settings.current()).thenReturn(new AiSettingsDTO("m", 1, 1000, null, "g", "g", 1000, null, null, null));
        TransactionTemplate tx = mock(TransactionTemplate.class);
        when(tx.execute(any())).thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
        service = new AiChatService(openRouter, mock(AiPromptRepository.class), mock(AiConversationRepository.class),
                mock(AiMessageRepository.class), mock(TaskRepository.class), mock(ExplanationStepRepository.class),
                mock(StorageService.class), tx, settings);
        account = mock(Account.class);
        when(account.getAccountId()).thenReturn(7L);
    }

    private static AiChatRequestDTO question() {
        return new AiChatRequestDTO(null, null, null, "Što je derivacija?", null, List.of());
    }

    private List<ServerSentEvent<String>> ask() {
        return service.chat(account, question()).collectList().block(Duration.ofSeconds(5));
    }

    private JsonNode chunk(String content) throws Exception {
        return mapper.readTree("{\"choices\":[{\"delta\":{\"content\":\"" + content + "\"}}]}");
    }

    @Test
    void failedAnswerIsNotCounted() {
        when(openRouter.stream(any())).thenReturn(Flux.error(new RuntimeException("OpenRouter 502")));

        List<ServerSentEvent<String>> events = ask();

        assertEquals("error", events.get(events.size() - 1).event());
        assertEquals(1, service.usage(account).remaining());
    }

    @Test
    void emptyAnswerIsNotCounted() {
        when(openRouter.stream(any())).thenReturn(Flux.empty());

        List<ServerSentEvent<String>> events = ask();

        assertEquals("error", events.get(events.size() - 1).event());
        assertEquals(1, service.usage(account).remaining());
    }

    @Test
    void answeredQuestionIsCountedUntilItLeavesTheWindow() throws Exception {
        when(openRouter.stream(any())).thenReturn(Flux.just(chunk("Bok")));

        List<ServerSentEvent<String>> events = ask();

        // Ping ne drži stream otvorenim nakon done.
        assertEquals("done", events.get(events.size() - 1).event());
        AiUsageDTO usage = service.usage(account);
        assertEquals(0, usage.remaining());
        Instant expected = Instant.now().plus(Duration.ofHours(24));
        assertTrue(Duration.between(usage.resetAt(), expected).abs().getSeconds() < 5);

        ResponseStatusException limited = assertThrows(ResponseStatusException.class, this::ask);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, limited.getStatusCode());
    }

    @Test
    void usageHasNoResetTimeWhileQuestionsRemain() {
        AiUsageDTO usage = service.usage(account);
        assertEquals(1, usage.limit());
        assertEquals(1, usage.remaining());
        assertNull(usage.resetAt());
    }
}
