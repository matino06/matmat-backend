package com.example.EduSprint.dto;

import java.time.Instant;

/**
 * The account's AI consent: {@code granted} is null until the student decides. {@code version} is the version of
 * the consent text they saw, {@code source} where they decided ("ios" or "web").
 */
public record AiConsentDTO(Boolean granted, String version, Instant decidedAt, String source) {
}
