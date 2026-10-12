package com.example.EduSprint.dto;

// Zahtjev za presigned URL: datoteka ide izravno iz preglednika na S3.
public record MaterialUploadRequest(String fileName, String contentType, Long sizeBytes) {
}
