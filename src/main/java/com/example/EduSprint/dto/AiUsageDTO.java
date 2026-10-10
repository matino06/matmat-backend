package com.example.EduSprint.dto;

import java.time.Instant;

/**
 * Potrošnja AI chata u zadnja 24 h.
 * resetAt: kad se oslobađa sljedeće pitanje; zadano samo kad je remaining 0.
 */
public record AiUsageDTO(int limit, int remaining, Instant resetAt) {
}
