package com.example.EduSprint.dto;

import java.util.List;

// Pitanje za admin uređivanje: sve što vidi učenik + točan odgovor, objašnjenje i slike rješenja.
public record AdminMockExamQuestionDTO(
        Long questionId,
        String questionNumber,
        String questionType,
        String questionText,
        Short points,
        Short sortOrder,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctOption,
        String correctAnswer,
        String answerNotes,
        String solutionExplanation,
        List<MockExamQuestionImageDTO> images,
        List<AdminMockExamQuestionDTO> subQuestions) {
}
