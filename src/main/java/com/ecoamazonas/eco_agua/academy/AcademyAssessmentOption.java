package com.ecoamazonas.eco_agua.academy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "academy_assessment_option",
    uniqueConstraints = @UniqueConstraint(name = "uk_academy_option_question_order", columnNames = {"question_id", "display_order"}),
    indexes = @Index(name = "idx_academy_option_question", columnList = "question_id")
)
public class AcademyAssessmentOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private AcademyAssessmentQuestion question;

    @Column(name = "option_text", nullable = false, length = 800)
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    private boolean correct = false;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AcademyAssessmentQuestion getQuestion() {
        return question;
    }

    public void setQuestion(AcademyAssessmentQuestion question) {
        this.question = question;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
