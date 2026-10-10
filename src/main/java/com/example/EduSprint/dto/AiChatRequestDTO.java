package com.example.EduSprint.dto;

import java.util.List;

/**
 * conversationId: null = novi razgovor (sprema se samo kad je taskId zadan).
 * taskId: null za pitanja o probnoj maturi ili općenita pitanja (ne spremaju se).
 * history: koristi se samo za razgovore koji se ne spremaju; za zadatke se povijest čita iz baze.
 * solutionRevealed: je li učenik otvorio rješenje zadatka; prije toga AI daje hintove. null = nepoznato.
 */
public record AiChatRequestDTO(Long conversationId,
                               Long taskId,
                               Boolean solutionRevealed,
                               String question,
                               Quote quote,
                               List<HistoryMessage> history) {

    /** source: "task" | "solution" | "exam" */
    public record Quote(String source, String text, List<String> imageUrls) {
    }

    /** role: "user" | "assistant" */
    public record HistoryMessage(String role, String content) {
    }
}
