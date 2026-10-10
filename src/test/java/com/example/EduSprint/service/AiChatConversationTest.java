package com.example.EduSprint.service;

import com.example.EduSprint.dto.AiChatRequestDTO;
import com.example.EduSprint.dto.AiSettingsDTO;
import com.example.EduSprint.dto.AiUsageDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.AiConversation;
import com.example.EduSprint.entity.AiMessage;
import com.example.EduSprint.entity.MockExam;
import com.example.EduSprint.entity.MockExamAttempt;
import com.example.EduSprint.entity.MockExamQuestion;
import com.example.EduSprint.entity.Task;
import com.example.EduSprint.repository.AiConversationRepository;
import com.example.EduSprint.repository.AiMessageRepository;
import com.example.EduSprint.repository.AiPromptRepository;
import com.example.EduSprint.repository.ExplanationStepRepository;
import com.example.EduSprint.repository.MockExamAttemptRepository;
import com.example.EduSprint.repository.MockExamQuestionRepository;
import com.example.EduSprint.repository.TaskRepository;
import com.example.EduSprint.storage.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Svi razgovori (zadatak, probna matura, općenito) se spremaju; baza je zamijenjena mockovima.
class AiChatConversationTest {

    private static final long ACCOUNT_ID = 7L;
    private static final long CONVERSATION_ID = 11L;

    private final ObjectMapper mapper = new ObjectMapper();
    private OpenRouterClient openRouter;
    private AiConversationRepository conversations;
    private AiMessageRepository messages;
    private MockExamAttemptRepository attempts;
    private MockExamQuestionRepository questions;
    private AiChatService service;
    private Account account;
    private final List<AiMessage> savedMessages = new ArrayList<>();
    private AiConversation savedConversation;

    @BeforeEach
    void setUp() throws Exception {
        openRouter = mock(OpenRouterClient.class);
        when(openRouter.stream(any())).thenReturn(Flux.just(chunk("Bok")));
        AiSettingsService settings = mock(AiSettingsService.class);
        when(settings.current()).thenReturn(new AiSettingsDTO("m", 1, 1000, null, "g", "g", 1000, null, null, null));
        TransactionTemplate tx = mock(TransactionTemplate.class);
        when(tx.execute(any())).thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
        doCallRealMethod().when(tx).executeWithoutResult(any());

        conversations = mock(AiConversationRepository.class);
        when(conversations.save(any())).thenAnswer(inv -> {
            AiConversation c = inv.getArgument(0);
            if (c.getConversationId() == null) ReflectionTestUtils.setField(c, "conversationId", CONVERSATION_ID);
            savedConversation = c;
            return c;
        });
        when(conversations.findById(CONVERSATION_ID)).thenAnswer(inv -> Optional.ofNullable(savedConversation));

        messages = mock(AiMessageRepository.class);
        when(messages.save(any())).thenAnswer(inv -> {
            AiMessage m = inv.getArgument(0);
            if (m.getMessageId() == null) ReflectionTestUtils.setField(m, "messageId", (long) savedMessages.size() + 1);
            savedMessages.add(m);
            return m;
        });
        when(messages.findById(anyLong())).thenAnswer(inv -> savedMessages.stream()
                .filter(m -> m.getMessageId().equals(inv.getArgument(0))).findFirst());

        attempts = mock(MockExamAttemptRepository.class);
        questions = mock(MockExamQuestionRepository.class);
        service = new AiChatService(openRouter, mock(AiPromptRepository.class), conversations, messages,
                mock(TaskRepository.class), mock(ExplanationStepRepository.class), attempts, questions,
                mock(StorageService.class), tx, settings);
        account = mock(Account.class);
        when(account.getAccountId()).thenReturn(ACCOUNT_ID);
    }

    private static AiChatRequestDTO request(Long conversationId, Long taskId, AiChatRequestDTO.Quote quote,
                                            Long attemptId, Long questionId) {
        return new AiChatRequestDTO(conversationId, taskId, null, "Što je derivacija?", quote, attemptId, questionId, null);
    }

    private static AiChatRequestDTO.Quote examQuote() {
        return new AiChatRequestDTO.Quote("exam", "Pitanje 3…", List.of());
    }

    private List<ServerSentEvent<String>> ask(AiChatRequestDTO req) {
        return service.chat(account, req).collectList().block(Duration.ofSeconds(5));
    }

    private JsonNode chunk(String content) throws Exception {
        return mapper.readTree("{\"choices\":[{\"delta\":{\"content\":\"" + content + "\"}}]}");
    }

    private JsonNode data(List<ServerSentEvent<String>> events, String name) throws Exception {
        for (ServerSentEvent<String> e : events) {
            if (name.equals(e.event())) return mapper.readTree(e.data());
        }
        throw new AssertionError("no " + name + " event");
    }

    private String systemPrompt() {
        ArgumentCaptor<ObjectNode> body = ArgumentCaptor.forClass(ObjectNode.class);
        verify(openRouter).stream(body.capture());
        return body.getValue().path("messages").path(0).path("content").asText();
    }

    private static MockExam exam(long id) {
        MockExam exam = mock(MockExam.class);
        when(exam.getExamId()).thenReturn(id);
        return exam;
    }

    private AiConversation existingConversation(Task task) {
        AiConversation conversation = new AiConversation(account, task, null);
        ReflectionTestUtils.setField(conversation, "conversationId", CONVERSATION_ID);
        savedConversation = conversation;
        when(conversations.findByConversationIdAndAccount_AccountId(CONVERSATION_ID, ACCOUNT_ID))
                .thenReturn(Optional.of(conversation));
        return conversation;
    }

    @Test
    void generalQuestionIsSaved() throws Exception {
        List<ServerSentEvent<String>> events = ask(request(null, null, null, null, null));

        assertEquals(CONVERSATION_ID, data(events, "meta").path("conversationId").asLong());
        assertEquals(AiConversation.CONTEXT_GENERAL, savedConversation.getContextType());
        assertNull(savedConversation.getTask());
        assertEquals(2, savedMessages.size());
        assertEquals(AiMessage.ROLE_USER, savedMessages.get(0).getRole());
        assertEquals("Bok", savedMessages.get(1).getContent());
        // Odgovor se može ocijeniti.
        assertEquals(savedMessages.get(1).getMessageId(), data(events, "done").path("messageId").asLong());
    }

    @Test
    void examQuestionIsLinkedToAttemptAndQuestion() {
        MockExam exam = exam(5L);
        MockExamAttempt attempt = mock(MockExamAttempt.class);
        when(attempt.getExam()).thenReturn(exam);
        MockExamQuestion question = mock(MockExamQuestion.class);
        when(question.getExam()).thenReturn(exam);
        when(attempts.findByAttemptIdAndAccount_AccountId(3L, ACCOUNT_ID)).thenReturn(Optional.of(attempt));
        when(questions.findById(4L)).thenReturn(Optional.of(question));

        ask(request(null, null, examQuote(), 3L, 4L));

        assertEquals(AiConversation.CONTEXT_EXAM, savedConversation.getContextType());
        assertSame(attempt, savedConversation.getMockExamAttempt());
        assertSame(question, savedConversation.getMockExamQuestion());
        assertTrue(systemPrompt().contains("probne državne mature"));
    }

    @Test
    void examQuestionWithoutIdsIsStillAnExamConversation() {
        ask(request(null, null, examQuote(), null, null));

        assertEquals(AiConversation.CONTEXT_EXAM, savedConversation.getContextType());
        assertNull(savedConversation.getMockExamAttempt());
    }

    @Test
    void followUpInExamConversationKeepsExamContextAndStoredHistory() {
        AiConversation conversation = existingConversation(null);
        conversation.attachExam(null, null);
        AiMessage earlier = new AiMessage(conversation, AiMessage.ROLE_USER, "Pitanje s probne mature: …");
        when(messages.findByConversation_ConversationIdAndStatusInOrderByCreatedAtAsc(eq(CONVERSATION_ID), anyList()))
                .thenReturn(List.of(earlier));

        ask(request(CONVERSATION_ID, null, null, null, null));

        ArgumentCaptor<ObjectNode> body = ArgumentCaptor.forClass(ObjectNode.class);
        verify(openRouter).stream(body.capture());
        assertTrue(body.getValue().path("messages").path(0).path("content").asText().contains("probne državne mature"));
        assertEquals("Pitanje s probne mature: …", body.getValue().path("messages").path(1).path("content").asText());
    }

    @Test
    void attemptOfAnotherStudentIsNotFound() {
        when(attempts.findByAttemptIdAndAccount_AccountId(3L, ACCOUNT_ID)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> ask(request(null, null, examQuote(), 3L, null)));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        assertTrue(savedMessages.isEmpty());
    }

    @Test
    void questionFromAnotherExamIsNotFound() {
        MockExam attemptExam = exam(5L);
        MockExam otherExam = exam(6L);
        MockExamAttempt attempt = mock(MockExamAttempt.class);
        when(attempt.getExam()).thenReturn(attemptExam);
        MockExamQuestion question = mock(MockExamQuestion.class);
        when(question.getExam()).thenReturn(otherExam);
        when(attempts.findByAttemptIdAndAccount_AccountId(3L, ACCOUNT_ID)).thenReturn(Optional.of(attempt));
        when(questions.findById(4L)).thenReturn(Optional.of(question));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> ask(request(null, null, examQuote(), 3L, 4L)));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void taskConversationIsNotContinuedWithoutTheTask() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(9);
        existingConversation(task);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> ask(request(CONVERSATION_ID, null, null, null, null)));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void conversationWithoutTaskIsNotContinuedOnATask() {
        existingConversation(null);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> ask(request(CONVERSATION_ID, 9L, null, null, null)));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void failedAnswerMarksTheQuestionSoItIsNotCounted() {
        when(openRouter.stream(any())).thenReturn(Flux.error(new RuntimeException("OpenRouter 502")));

        List<ServerSentEvent<String>> events = ask(request(null, null, null, null, null));

        assertEquals("error", events.get(events.size() - 1).event());
        assertEquals(AiMessage.STATUS_ERROR, savedMessages.get(0).getStatus());
    }

    @Test
    void storedQuestionsCountUntilTheyLeaveTheWindow() {
        Instant asked = Instant.now().minus(Duration.ofHours(1));
        when(messages.findQuestionTimesSince(eq(ACCOUNT_ID), any())).thenReturn(List.of(asked));

        AiUsageDTO usage = service.usage(account);
        assertEquals(0, usage.remaining());
        assertEquals(asked.plus(Duration.ofHours(24)), usage.resetAt());

        ResponseStatusException limited = assertThrows(ResponseStatusException.class,
                () -> ask(request(null, null, examQuote(), null, null)));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, limited.getStatusCode());
        assertTrue(savedMessages.isEmpty());
    }

    @Test
    void usageHasNoResetTimeWhileQuestionsRemain() {
        AiUsageDTO usage = service.usage(account);
        assertEquals(1, usage.limit());
        assertEquals(1, usage.remaining());
        assertNull(usage.resetAt());
    }
}
