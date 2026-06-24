package com.ecoamazonas.eco_agua.platform.control;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "matrix26_provisioning_step",
        indexes = @Index(name = "idx_matrix26_provisioning_step_job_order", columnList = "job_id, display_order")
)
public class Matrix26ProvisioningStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Matrix26ProvisioningJob job;

    @Column(name = "step_code", nullable = false, length = 80)
    private String stepCode;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false, length = 180)
    private String label;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 1000)
    private String detail;

    @Column(name = "safety_scope", nullable = false, length = 80)
    private String safetyScope;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Matrix26ProvisioningJob getJob() {
        return job;
    }

    public void setJob(Matrix26ProvisioningJob job) {
        this.job = job;
    }

    public String getStepCode() {
        return stepCode;
    }

    public void setStepCode(String stepCode) {
        this.stepCode = stepCode;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getSafetyScope() {
        return safetyScope;
    }

    public void setSafetyScope(String safetyScope) {
        this.safetyScope = safetyScope;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
