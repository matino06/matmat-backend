package com.example.EduSprint.dto;

/** reason: "incorrect" (netočno) or "inappropriate" (neprikladno); note: optional, what the student adds. */
public record AiReportRequestDTO(String reason, String note) {
}
