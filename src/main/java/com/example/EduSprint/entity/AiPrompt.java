package com.example.EduSprint.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "ai_prompt")
public class AiPrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prompt_id", nullable = false)
    private Long promptId;

    // NULL = osnovni prompt, inače dodatak za predmet (course.subject)
    @Column(name = "subject", length = 30)
    private String subject;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    public AiPrompt() {
    }

    public Long getPromptId() {
        return promptId;
    }

    public String getSubject() {
        return subject;
    }

    public Integer getVersion() {
        return version;
    }

    public String getContent() {
        return content;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
