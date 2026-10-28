package com.example.EduSprint.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "course")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "course_description")
    private String courseDescription;

    @Column(name = "subject", length = 30)
    private String subject;

    // Vlastiti predmet: vlasnik je kreator; null znači službeni predmet. Samo id, da se
    // predmet može vraćati kao JSON bez povlačenja cijelog računa.
    @Column(name = "owner_account_id")
    private Long ownerAccountId;

    // Stanje izrade vlastitog predmeta (nov | izrada | nacrt | potvrden); null za službene.
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "learning_mode", length = 20)
    private String learningMode;

    // Oblik lekcija vlastitog predmeta: tekst | interaktivno (uz tekst i lekcija od widgeta).
    @Column(name = "lesson_format", length = 20, nullable = false)
    private String lessonFormat = "tekst";

    // Razina vlastitog predmeta (os78 | ss); null za službene.
    @Column(name = "level", length = 20)
    private String level;

    @Column(name = "exam_date")
    private LocalDate examDate;

    // Službeni predmet koji aplikacije nude (sql/2026-10-28-predmeti-iz-baze.sql); neobjavljene vidi samo admin.
    @Column(name = "published", nullable = false)
    private boolean published;

    // Kako ga aplikacije prikazuju: redoslijed u izborniku, oznaka i boja (hsl nijansa), predmet bez razine
    // („Matematika“), razina mature (A | B) s opisom, podnaslov i PDF formula na webu. Za vlastite predmete null.
    @Column(name = "position")
    private Integer position;

    @Column(name = "badge", length = 8)
    private String badge;

    @Column(name = "hue")
    private Integer hue;

    @Column(name = "short_name", length = 60)
    private String shortName;

    @Column(name = "matura_level", length = 5)
    private String maturaLevel;

    @Column(name = "level_hint", length = 200)
    private String levelHint;

    @Column(name = "tagline", length = 120)
    private String tagline;

    @Column(name = "formula_pdf", length = 200)
    private String formulaPdf;

    public Course(String courseName, String courseDescription) {
        this.courseName = courseName;
        this.courseDescription = courseDescription;
    }

    public Course() {
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseDescription() {
        return courseDescription;
    }

    public void setCourseDescription(String courseDescription) {
        this.courseDescription = courseDescription;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Long getOwnerAccountId() {
        return ownerAccountId;
    }

    public void setOwnerAccountId(Long ownerAccountId) {
        this.ownerAccountId = ownerAccountId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLearningMode() {
        return learningMode;
    }

    public void setLearningMode(String learningMode) {
        this.learningMode = learningMode;
    }

    public String getLessonFormat() {
        return lessonFormat;
    }

    public void setLessonFormat(String lessonFormat) {
        this.lessonFormat = lessonFormat;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public Integer getPosition() {
        return position;
    }

    public String getBadge() {
        return badge;
    }

    public Integer getHue() {
        return hue;
    }

    public String getShortName() {
        return shortName;
    }

    public String getMaturaLevel() {
        return maturaLevel;
    }

    public String getLevelHint() {
        return levelHint;
    }

    public String getTagline() {
        return tagline;
    }

    public String getFormulaPdf() {
        return formulaPdf;
    }

    public boolean isOwnedBy(Long accountId) {
        return ownerAccountId != null && ownerAccountId.equals(accountId);
    }
}
