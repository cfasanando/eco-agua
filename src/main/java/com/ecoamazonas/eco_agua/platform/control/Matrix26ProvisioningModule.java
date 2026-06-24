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
        name = "matrix26_provisioning_module",
        indexes = @Index(name = "idx_matrix26_provisioning_module_job", columnList = "job_id, module_key")
)
public class Matrix26ProvisioningModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Matrix26ProvisioningJob job;

    @Column(name = "module_key", nullable = false, length = 80)
    private String moduleKey;

    @Column(name = "module_name", nullable = false, length = 150)
    private String moduleName;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "installer_available", nullable = false)
    private boolean installerAvailable;

    @Column(name = "installer_version", length = 50)
    private String installerVersion;

    @Column(columnDefinition = "TEXT")
    private String detail;

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

    public String getModuleKey() {
        return moduleKey;
    }

    public void setModuleKey(String moduleKey) {
        this.moduleKey = moduleKey;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isInstallerAvailable() {
        return installerAvailable;
    }

    public void setInstallerAvailable(boolean installerAvailable) {
        this.installerAvailable = installerAvailable;
    }

    public String getInstallerVersion() {
        return installerVersion;
    }

    public void setInstallerVersion(String installerVersion) {
        this.installerVersion = installerVersion;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
