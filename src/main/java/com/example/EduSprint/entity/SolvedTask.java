package com.example.EduSprint.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "solved_task")
public class SolvedTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "q", nullable = false)
    private Short q;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    public SolvedTask(Account account, Task task, Short q, Instant startTime, Instant endTime, Course course) {
        this.account = account;
        this.task = task;
        this.q = q;
        this.startTime = startTime;
        this.endTime = endTime;
        this.course = course;
    }

    public SolvedTask() {
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Short getQ() {
        return q;
    }

    public void setQ(Short q) {
        this.q = q;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    @Override
    public String toString() {
        return "SolvedTask{" +
                "id=" + id +
                ", account=" + account +
                ", task=" + task +
                ", q=" + q +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}