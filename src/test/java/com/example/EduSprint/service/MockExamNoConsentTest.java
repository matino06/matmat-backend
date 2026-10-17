package com.example.EduSprint.service;

import com.example.EduSprint.dto.MockExamAnswerSubmissionDTO;
import com.example.EduSprint.dto.MockExamSubmitRequestDTO;
import com.example.EduSprint.dto.MockExamSubmitResponseDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.MockExam;
import com.example.EduSprint.entity.MockExamAnswer;
import com.example.EduSprint.entity.MockExamAttempt;
import com.example.EduSprint.entity.MockExamQuestion;
import com.example.EduSprint.repository.*;
import com.example.EduSprint.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** A mock exam submitted without AI consent: multiple choice is graded, the rest is kept but not sent to the AI. */
class MockExamNoConsentTest {

    private final MockExamRepository exams = mock(MockExamRepository.class);
    private final MockExamQuestionRepository questions = mock(MockExamQuestionRepository.class);
    private final MockExamAttemptRepository attempts = mock(MockExamAttemptRepository.class);
    private final MockExamAnswerRepository answers = mock(MockExamAnswerRepository.class);
    private final AiGradingService ai = mock(AiGradingService.class);
    private final MockExamGradingService service = new MockExamGradingService(exams, questions, attempts, answers,
            mock(MockExamScoringCriterionRepository.class), mock(MockExamCriterionScoreRepository.class),
            mock(StorageService.class), ai, null);

    private MockExamQuestion question(long id, String type, short points, String correct) {
        MockExamQuestion q = new MockExamQuestion();
        ReflectionTestUtils.setField(q, "questionId", id);
        q.setQuestionType(type);
        q.setPoints(points);
        q.setCorrectOption(correct);
        return q;
    }

    private MockExamAnswerSubmissionDTO answer(long questionId, String option, String text) {
        MockExamAnswerSubmissionDTO a = new MockExamAnswerSubmissionDTO();
        a.setQuestionId(questionId);
        a.setSelectedOption(option);
        a.setAnswerText(text);
        return a;
    }

    private List<MockExamAnswer> submit(boolean aiAllowed, MockExamSubmitResponseDTO[] response) {
        MockExam exam = new MockExam();
        exam.setIsPublished(true);
        when(exams.findById(7L)).thenReturn(Optional.of(exam));
        when(questions.findByExam_ExamIdOrderBySortOrderAsc(7L)).thenReturn(List.of(
                question(1, "multiple_choice", (short) 1, "B"), question(2, "short_answer", (short) 2, null)));
        when(attempts.save(any())).thenAnswer(inv -> {
            MockExamAttempt a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "attemptId", 99L);
            return a;
        });
        MockExamSubmitRequestDTO request = new MockExamSubmitRequestDTO();
        request.setAnswers(List.of(answer(1, "B", null), answer(2, null, "x = 3")));
        response[0] = service.submit(new Account(), 7L, request, Map.of(), aiAllowed);
        ArgumentCaptor<MockExamAnswer> saved = ArgumentCaptor.forClass(MockExamAnswer.class);
        verify(answers, atLeastOnce()).save(saved.capture());
        return saved.getAllValues();
    }

    @Test
    void withoutConsentOnlyMultipleChoiceIsGraded() {
        MockExamSubmitResponseDTO[] response = new MockExamSubmitResponseDTO[1];
        List<MockExamAnswer> saved = submit(false, response);

        assertEquals("graded", response[0].getGradingStatus());
        assertEquals("not_needed", saved.get(0).getAiGradingStatus());
        assertEquals((short) 1, saved.get(0).getScoreAwarded());
        assertEquals(MockExamGradingService.STATUS_NO_CONSENT, saved.get(1).getAiGradingStatus());
        assertEquals((short) 0, saved.get(1).getScoreAwarded());
        assertEquals("Nije ocijenjeno jer dozvola za AI nije bila uključena.", saved.get(1).getAiFeedback());
        assertEquals("x = 3", saved.get(1).getAnswerText());
        verifyNoInteractions(ai);
    }

    @Test
    void withConsentTheShortAnswerWaitsForTheAi() {
        MockExamSubmitResponseDTO[] response = new MockExamSubmitResponseDTO[1];
        List<MockExamAnswer> saved = submit(true, response);
        assertEquals("pending", response[0].getGradingStatus());
        assertEquals("pending", saved.get(1).getAiGradingStatus());
    }
}
