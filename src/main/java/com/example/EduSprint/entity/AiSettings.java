package com.example.EduSprint.entity;

import jakarta.persistence.*;

import java.time.Instant;

// Jedan red (id = 1) s AI postavkama koje admin mijenja s dashboarda. API ključevi su u env varijablama.
@Entity
@Table(name = "ai_settings")
public class AiSettings {

    public static final short ID = 1;

    @Id
    @Column(name = "id", nullable = false)
    private Short id;

    @Column(name = "chat_model", nullable = false, length = 200)
    private String chatModel;

    @Column(name = "chat_daily_limit", nullable = false)
    private Integer chatDailyLimit;

    @Column(name = "chat_max_tokens", nullable = false)
    private Integer chatMaxTokens;

    // NULL = effort se ne šalje, model razmišlja po svom zadanom
    @Column(name = "chat_reasoning_effort", length = 10)
    private String chatReasoningEffort;

    @Column(name = "grading_model_text", nullable = false, length = 200)
    private String gradingModelText;

    @Column(name = "grading_model_vision", nullable = false, length = 200)
    private String gradingModelVision;

    @Column(name = "grading_max_tokens", nullable = false)
    private Integer gradingMaxTokens;

    @Column(name = "grading_reasoning_effort", length = 10)
    private String gradingReasoningEffort;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Account updatedBy;

    public AiSettings() {
    }

    public Short getId() {
        return id;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public Integer getChatDailyLimit() {
        return chatDailyLimit;
    }

    public void setChatDailyLimit(Integer chatDailyLimit) {
        this.chatDailyLimit = chatDailyLimit;
    }

    public Integer getChatMaxTokens() {
        return chatMaxTokens;
    }

    public void setChatMaxTokens(Integer chatMaxTokens) {
        this.chatMaxTokens = chatMaxTokens;
    }

    public String getChatReasoningEffort() {
        return chatReasoningEffort;
    }

    public void setChatReasoningEffort(String chatReasoningEffort) {
        this.chatReasoningEffort = chatReasoningEffort;
    }

    public String getGradingModelText() {
        return gradingModelText;
    }

    public void setGradingModelText(String gradingModelText) {
        this.gradingModelText = gradingModelText;
    }

    public String getGradingModelVision() {
        return gradingModelVision;
    }

    public void setGradingModelVision(String gradingModelVision) {
        this.gradingModelVision = gradingModelVision;
    }

    public Integer getGradingMaxTokens() {
        return gradingMaxTokens;
    }

    public void setGradingMaxTokens(Integer gradingMaxTokens) {
        this.gradingMaxTokens = gradingMaxTokens;
    }

    public String getGradingReasoningEffort() {
        return gradingReasoningEffort;
    }

    public void setGradingReasoningEffort(String gradingReasoningEffort) {
        this.gradingReasoningEffort = gradingReasoningEffort;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Account getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Account updatedBy) {
        this.updatedBy = updatedBy;
    }
}
