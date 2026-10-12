package com.example.EduSprint.entity;

import com.example.EduSprint.config.AppTime;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Entity
public class Account {

    public static final int DEFAULT_DAILY_GOAL = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id", unique = true, nullable = false)
    private Long accountId;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;

    @Column(name = "count_weak_objectives", nullable = false)
    private Short countWeakObjectives;

    @OneToOne
    @JoinColumn(name="current_task_id")
    private Task currentTask;

    @ManyToOne
    @JoinColumn(name = "current_course_id")
    private Course currentCourse;

    @JoinColumn(name = "learning_reminders_enabled", nullable = true)
    private Boolean learningRemindersEnabled;

    @JoinColumn(name = "feature_announcements_enabled", nullable = true)
    private Boolean featureAnnouncementsEnabled;

    @JoinColumn(name = "tempo", nullable = false)
    private Short tempo;

    @Column(name = "is_admin", nullable = false)
    private Boolean isAdmin;

    // Smije izrađivati vlastite predmete; postavlja se ručno, kao is_admin.
    @Column(name = "is_creator", nullable = false)
    private Boolean isCreator = false;

    // Mjesečna kvota izrade vlastitih predmeta, u dolarima troška OpenRoutera.
    @Column(name = "monthly_quota_usd", nullable = false, precision = 8, scale = 2)
    private BigDecimal monthlyQuotaUsd = BigDecimal.TEN;

    // One goal across all courses: tasks solved in any course count towards it.
    @Column(name = "daily_goal", nullable = false)
    private Integer dailyGoal;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserCourseGoal> courseGoals;

    @OneToMany
    @JoinTable(name = "weak_objectives", joinColumns = @JoinColumn(name = "account_id"), inverseJoinColumns = @JoinColumn(name = "objective_id"))
    private List<LearningObjective> weakObjectives;

    public Account(String email, String name, Course currentCourse) {
        this.email = email;
        this.name = name;
        this.registrationDate = AppTime.today();
        this.countWeakObjectives = 0;
        this.currentCourse = currentCourse;
        this.learningRemindersEnabled = null;
        this.featureAnnouncementsEnabled = null;
        this.tempo = 2;
        this.isAdmin = false;
        this.dailyGoal = DEFAULT_DAILY_GOAL;
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

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
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

    public Course getCurrentCourse() {
        return currentCourse;
    }

    public void setCurrentCourse(Course currentCourse) {
        this.currentCourse = currentCourse;
    }

    public Boolean isLearningRemindersEnabled() {
        return learningRemindersEnabled;
    }

    public void setLearningRemindersEnabled(Boolean learningRemindersEnabled) {
        this.learningRemindersEnabled = learningRemindersEnabled;
    }

    public Boolean isFeatureAnnouncementsEnabled() {
        return featureAnnouncementsEnabled;
    }

    public void setFeatureAnnouncementsEnabled(Boolean featureAnnouncementsEnabled) {
        this.featureAnnouncementsEnabled = featureAnnouncementsEnabled;
    }

    public List<UserCourseGoal> getCourseGoals() {
        return courseGoals;
    }

    public void setCourseGoals(List<UserCourseGoal> courseGoals) {
        this.courseGoals = courseGoals;
    }

    public Short getTempo() {
        return tempo;
    }

    public void setTempo(Short tempo) {
        this.tempo = tempo;
    }

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public Boolean getIsCreator() {
        return isCreator;
    }

    public void setIsCreator(Boolean isCreator) {
        this.isCreator = isCreator;
    }

    public BigDecimal getMonthlyQuotaUsd() {
        return monthlyQuotaUsd;
    }

    public void setMonthlyQuotaUsd(BigDecimal monthlyQuotaUsd) {
        this.monthlyQuotaUsd = monthlyQuotaUsd;
    }

    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public Integer getDailyGoal() {
        return dailyGoal;
    }

    public void setDailyGoal(Integer dailyGoal) {
        this.dailyGoal = dailyGoal;
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

