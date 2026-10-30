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

    // ocjena 1–4 (ponovno, teško, dobro, lako); stari stupac q (0–5, SM-2) ostaje samo u povijesti
    @Column(name = "grade", nullable = false)
    private Short grade;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name="progress")
    private Short progress;

    @Column(name="device")
    private String device;

    @Column(name="tempo")
    private Short tempo;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    // Stanje lekcije (account_objective) prije ove ocjene, da se ocjena smije promijeniti: vrati se ovo stanje pa
    // se ocjena primijeni ponovno. Starije ocjene ga nemaju (prevN null).
    @Column(name = "prev_i")
    private Short prevI;

    @Column(name = "prev_n")
    private Short prevN;

    @Column(name = "prev_stability")
    private Float prevStability;

    @Column(name = "prev_difficulty")
    private Float prevDifficulty;

    @Column(name = "prev_last_grade")
    private Short prevLastGrade;

    @Column(name = "prev_last_solved_date")
    private Instant prevLastSolvedDate;

    public SolvedTask(Account account, Task task, Short grade, Instant startTime, Instant endTime, Course course, Short progress, String device, Short tempo) {
        this.account = account;
        this.task = task;
        this.grade = grade;
        this.startTime = startTime;
        this.endTime = endTime;
        this.progress = progress;
        this.device = device;
        this.course = course;
        this.tempo = tempo;
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

    public Short getGrade() {
        return grade;
    }

    public void setGrade(Short grade) {
        this.grade = grade;
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

    public Short getProgress() {
        return progress;
    }

    public void setProgress(Short progress) {
        this.progress = progress;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Short getTempo() {
        return tempo;
    }

    public void setTempo(Short tempo) {
        this.tempo = tempo;
    }

    /** Zapamti stanje lekcije prije ove ocjene. */
    public void rememberBefore(AccountObjective state) {
        this.prevI = state.getI();
        this.prevN = state.getN();
        this.prevStability = state.getStability();
        this.prevDifficulty = state.getDifficulty();
        this.prevLastGrade = state.getLastGrade();
        this.prevLastSolvedDate = state.getLastSolvedDate();
    }

    /** Ima li ocjena zapamćeno stanje prije sebe, pa se smije promijeniti. */
    public boolean remembersBefore() {
        return prevN != null;
    }

    /** Vrati lekciji stanje prije ove ocjene. */
    public void restoreBefore(AccountObjective state) {
        state.setI(prevI);
        state.setN(prevN);
        state.setStability(prevStability);
        state.setDifficulty(prevDifficulty);
        state.setLastGrade(prevLastGrade);
        state.setLastSolvedDate(prevLastSolvedDate);
    }

    @Override
    public String toString() {
        return "SolvedTask{" +
                "id=" + id +
                ", account=" + account +
                ", task=" + task +
                ", grade=" + grade +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", progress=" + progress +
                ", device='" + device + '\'' +
                ", course=" + course +
                '}';
    }
}