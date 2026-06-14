package com.ecoamazonas.eco_agua.academy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "academy_assessment_answer",
    uniqueConstraints = @UniqueConstraint(name = "uk_academy_answer_attempt_question", columnNames = {"attempt_id", "question_id"}),
    indexes = {
        @Index(name = "idx_academy_answer_attempt", columnList = "attempt_id"),
        @Index(name = "idx_academy_answer_question", columnList = "question_id")
    }
)
public class AcademyAssessmentAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private AcademyAssessmentAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private AcademyAssessmentQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private AcademyAssessmentOption selectedOption;

    @Lob
    @Column(name = "answer_text")
    private String answerText;

    @Column(name = "is_correct", nullable = false)
    private boolean correct = false;

    @Column(name = "points_awarded", nullable = false)
    private int pointsAwarded = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AcademyAssessmentAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(AcademyAssessmentAttempt attempt) {
        this.attempt = attempt;
    }

    public AcademyAssessmentQuestion getQuestion() {
        return question;
    }

    public void setQuestion(AcademyAssessmentQuestion question) {
        this.question = question;
    }

    public AcademyAssessmentOption getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(AcademyAssessmentOption selectedOption) {
        this.selectedOption = selectedOption;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }

    public void setPointsAwarded(int pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
