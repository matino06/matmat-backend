package com.example.EduSprint.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "account_objective")
public class AccountObjective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne
    @JoinColumn(name = "objective_id", nullable = false)
    private LearningObjective objective;

    @ColumnDefault("2.5")
    @Column(name = "ef", nullable = false)
    private Float EF;

    @ColumnDefault("0")
    @Column(name = "i", nullable = false)
    private Short I;

    @ColumnDefault("0")
    @Column(name = "n", nullable = false)
    private Short n;

    @Column(name = "last_q")
    private Short lastQ;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "last_solved_date", nullable = false)
    private Instant lastSolvedDate;

    public AccountObjective() {
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

    public LearningObjective getObjective() {
        return objective;
    }

    public void setObjective(LearningObjective objective) {
        this.objective = objective;
    }

    public Float getEf() {
        return EF;
    }

    public void setEf(Float EF) {
        this.EF = EF;
    }

    public Short getI() {
        return I;
    }

    public void setI(Short I) {
        this.I = I;
    }

    public Short getN() {
        return n;
    }

    public void setN(Short n) {
        this.n = n;
    }

    public Short getLastQ() {
        return lastQ;
    }

    public void setLastQ(Short lastQ) {
        this.lastQ = lastQ;
    }

    public Instant getLastSolvedDate() {
        return lastSolvedDate;
    }

    public void setLastSolvedDate(Instant lastSolvedDate) {
        this.lastSolvedDate = lastSolvedDate;
    }

    @Override
    public String toString() {
        return "AccountObjective{" + "id=" + id + "}";
    }
}