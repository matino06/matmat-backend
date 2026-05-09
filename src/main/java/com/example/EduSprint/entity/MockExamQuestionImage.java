package com.example.EduSprint.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "mock_exam_question_image")
public class MockExamQuestionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id", unique = true, nullable = false)
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private MockExamQuestion question;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "alt_text", columnDefinition = "TEXT")
    private String altText;

    @Column(name = "image_context", nullable = false)
    private String imageContext;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public MockExamQuestionImage() {
    }

    public Long getImageId() {
        return imageId;
    }

    public MockExamQuestion getQuestion() {
        return question;
    }

    public void setQuestion(MockExamQuestion question) {
        this.question = question;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public String getImageContext() {
        return imageContext;
    }

    public void setImageContext(String imageContext) {
        this.imageContext = imageContext;
    }

    public Short getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Short sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
