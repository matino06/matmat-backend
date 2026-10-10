package com.example.EduSprint.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "ai_conversation")
public class AiConversation {

    public static final String CONTEXT_TASK = "task";
    public static final String CONTEXT_EXAM = "exam";
    public static final String CONTEXT_GENERAL = "general";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(name = "context_type", nullable = false, length = 10)
    private String contextType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_exam_attempt_id")
    private MockExamAttempt mockExamAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_exam_question_id")
    private MockExamQuestion mockExamQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AiConversation() {
    }

    /** task: null za razgovor o probnoj maturi ili općeniti razgovor. */
    public AiConversation(Account account, Task task, Course course) {
        this.account = account;
        this.task = task;
        this.contextType = task != null ? CONTEXT_TASK : CONTEXT_GENERAL;
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

    public String getContextType() {
        return contextType;
    }

    public boolean isExam() {
        return CONTEXT_EXAM.equals(contextType);
    }

    public MockExamAttempt getMockExamAttempt() {
        return mockExamAttempt;
    }

    public MockExamQuestion getMockExamQuestion() {
        return mockExamQuestion;
    }

    /**
     * Razgovor bez zadatka postaje razgovor o maturi čim učenik pita o pitanju s mature.
     * Pokušaj i pitanje su ono o čemu je zadnje pitao; null ako ih frontend nije poslao.
     */
    public void attachExam(MockExamAttempt attempt, MockExamQuestion question) {
        if (task != null) {
            throw new IllegalStateException("Razgovor o zadatku ne može biti vezan uz maturu");
        }
        this.contextType = CONTEXT_EXAM;
        if (attempt != null || question != null) {
            this.mockExamAttempt = attempt;
            this.mockExamQuestion = question;
        }
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
