package com.example.EduSprint.dto;

// Tekstualna polja pitanja koja admin može mijenjati; šalju se sva odjednom.
// Prazan string u opcionalnom polju sprema se kao null.
public record AdminMockExamQuestionUpdateDTO(
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctOption,
        String correctAnswer,
        String answerNotes,
        String solutionExplanation) {
}
