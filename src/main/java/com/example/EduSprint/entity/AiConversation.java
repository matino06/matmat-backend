package com.example.EduSprint.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "ai_conversation")
public class AiConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AiConversation() {
    }

    public AiConversation(Account account, Task task, Course course) {
        this.account = account;
        this.task = task;
        this.course = course;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public Account getAccount() {
        return account;
    }

    public Task getTask() {
        return task;
    }

    public Course getCourse() {
        return course;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
