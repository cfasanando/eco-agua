package com.ecoamazonas.eco_agua.academy;

import com.ecoamazonas.eco_agua.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "academy_assessment_attempt",
    indexes = {
        @Index(name = "idx_academy_attempt_assessment", columnList = "assessment_id"),
        @Index(name = "idx_academy_attempt_enrollment", columnList = "enrollment_id"),
        @Index(name = "idx_academy_attempt_student", columnList = "student_id"),
        @Index(name = "idx_academy_attempt_course", columnList = "course_id")
    }
)
public class AcademyAssessmentAttempt {

    public enum Status {
        IN_PROGRESS("En progreso"),
        SUBMITTED("Enviado");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private AcademyAssessment assessment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private AcademyEnrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private UserAccount student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private AcademyCourse course;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.IN_PROGRESS;

    @Column(nullable = false)
    private int score = 0;

    @Column(name = "max_score", nullable = false)
    private int maxScore = 0;

    @Column(nullable = false)
    private boolean passed = false;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @PrePersist
    public void prePersist() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    public int getPercentScore() {
        return maxScore > 0 ? (int) Math.round((score * 100.0d) / maxScore) : 0;
    }

    public String getStatusLabel() {
        return status != null ? status.getLabel() : Status.IN_PROGRESS.getLabel();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AcademyAssessment getAssessment() {
        return assessment;
    }

    public void setAssessment(AcademyAssessment assessment) {
        this.assessment = assessment;
    }

    public AcademyEnrollment getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(AcademyEnrollment enrollment) {
        this.enrollment = enrollment;
    }

    public UserAccount getStudent() {
        return student;
    }

    public void setStudent(UserAccount student) {
        this.student = student;
    }

    public AcademyCourse getCourse() {
        return course;
    }

    public void setCourse(AcademyCourse course) {
        this.course = course;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
