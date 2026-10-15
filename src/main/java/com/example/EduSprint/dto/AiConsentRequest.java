package com.example.EduSprint.dto;

public record AiConsentRequest(Boolean granted, String version, String source) {
}
