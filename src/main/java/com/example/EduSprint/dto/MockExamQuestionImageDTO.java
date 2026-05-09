package com.example.EduSprint.dto;

public class MockExamQuestionImageDTO {
    private String imageUrl;
    private String altText;
    private String imageContext;
    private Short sortOrder;

    public MockExamQuestionImageDTO(String imageUrl, String altText, String imageContext, Short sortOrder) {
        this.imageUrl = imageUrl;
        this.altText = altText;
        this.imageContext = imageContext;
        this.sortOrder = sortOrder;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getAltText() {
        return altText;
    }

    public String getImageContext() {
        return imageContext;
    }

    public Short getSortOrder() {
        return sortOrder;
    }
}
