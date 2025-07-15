package com.example.EduSprint.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id", unique = true, nullable = false)
    private Long accountId;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "count_weak_objectives", nullable = false)
    private Short countWeakObjectives;

    @OneToOne
    @JoinColumn(name="current_task_id")
    private Task currentTask;

    @OneToMany
    @JoinTable(name = "weak_objectives", joinColumns = @JoinColumn(name = "account_id"), inverseJoinColumns = @JoinColumn(name = "objective_id"))
    private List<LearningObjective> weakObjectives;

    public Account(String email, String name) {
        this.email = email;
        this.name = name;
        this.countWeakObjectives = 0;
    }

    public Account() {
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<LearningObjective> getWeakObjectives() {
        return weakObjectives;
    }

    public void setWeakObjectives(List<LearningObjective> weakObjectives) {
        this.weakObjectives = weakObjectives;
    }

    public Task getCurrentTask() {
        return currentTask;
    }

    public void setCurrentTask(Task currentTask) {
        this.currentTask = currentTask;
    }

    public Short getCountWeakObjectives() {
        return countWeakObjectives;
    }

    public void setCountWeakObjectives(Short countWeakObjectives) {
        this.countWeakObjectives = countWeakObjectives;
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

