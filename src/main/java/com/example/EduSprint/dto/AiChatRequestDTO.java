package com.example.EduSprint.dto;

import java.util.List;

/**
 * conversationId: null = novi razgovor. Svi razgovori se spremaju, a povijest se čita iz baze.
 * taskId: null za pitanja o probnoj maturi ili općenita pitanja.
 * solutionRevealed: je li učenik otvorio rješenje zadatka; prije toga AI daje hintove. null = nepoznato.
 * mockExamAttemptId, mockExamQuestionId: uz citat s probne mature (quote.source "exam"); null = nepoznato.
 * history: samo za stare verzije frontenda koje ne šalju conversationId za razgovore bez zadatka;
 * čita se samo kad se otvara novi razgovor.
 */
public record AiChatRequestDTO(Long conversationId,
                               Long taskId,
                               Boolean solutionRevealed,
                               String question,
                               Quote quote,
                               Long mockExamAttemptId,
                               Long mockExamQuestionId,
                               List<HistoryMessage> history) {

    /** source: "task" | "solution" | "exam" */
    public record Quote(String source, String text, List<String> imageUrls) {
    }

    /** role: "user" | "assistant" */
    public record HistoryMessage(String role, String content) {
    }
}
