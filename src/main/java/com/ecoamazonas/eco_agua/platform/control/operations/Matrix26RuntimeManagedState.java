package com.ecoamazonas.eco_agua.platform.control.operations;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "matrix26_runtime_state",
        indexes = {
                @Index(name = "idx_matrix26_runtime_state_code", columnList = "instance_code", unique = true),
                @Index(name = "idx_matrix26_runtime_state_runtime_key", columnList = "runtime_key", unique = true)
        }
)
public class Matrix26RuntimeManagedState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instance_id", nullable = false, unique = true)
    private PlatformBusinessClient instance;

    @Column(name = "runtime_key", nullable = false, unique = true, length = 80)
    private String runtimeKey;

    @Column(name = "instance_code", nullable = false, unique = true, length = 80)
    private String instanceCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_state", nullable = false, length = 30)
    private Matrix26ManagedRuntimeState currentState;

    @Column(name = "last_known_pid")
    private Long lastKnownPid;

    @Column(name = "process_started_at")
    private LocalDateTime processStartedAt;

    @Column(name = "last_online_at")
    private LocalDateTime lastOnlineAt;

    @Column(name = "last_stopped_at")
    private LocalDateTime lastStoppedAt;

    @Column(name = "last_operation_id")
    private Long lastOperationId;

    @Column(name = "standard_log_path", length = 500)
    private String standardLogPath;

    @Column(name = "error_log_path", length = 500)
    private String errorLogPath;

    @Column(name = "pid_file_path", length = 500)
    private String pidFilePath;

    @Column(length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (currentState == null) {
            currentState = Matrix26ManagedRuntimeState.UNKNOWN;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public PlatformBusinessClient getInstance() {
        return instance;
    }

    public void setInstance(PlatformBusinessClient instance) {
        this.instance = instance;
    }

    public String getRuntimeKey() {
        return runtimeKey;
    }

    public void setRuntimeKey(String runtimeKey) {
        this.runtimeKey = runtimeKey;
    }

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }

    public Matrix26ManagedRuntimeState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(Matrix26ManagedRuntimeState currentState) {
        this.currentState = currentState;
    }

    public Long getLastKnownPid() {
        return lastKnownPid;
    }

    public void setLastKnownPid(Long lastKnownPid) {
        this.lastKnownPid = lastKnownPid;
    }

    public LocalDateTime getProcessStartedAt() {
        return processStartedAt;
    }

    public void setProcessStartedAt(LocalDateTime processStartedAt) {
        this.processStartedAt = processStartedAt;
    }

    public LocalDateTime getLastOnlineAt() {
        return lastOnlineAt;
    }

    public void setLastOnlineAt(LocalDateTime lastOnlineAt) {
        this.lastOnlineAt = lastOnlineAt;
    }

    public LocalDateTime getLastStoppedAt() {
        return lastStoppedAt;
    }

    public void setLastStoppedAt(LocalDateTime lastStoppedAt) {
        this.lastStoppedAt = lastStoppedAt;
    }

    public Long getLastOperationId() {
        return lastOperationId;
    }

    public void setLastOperationId(Long lastOperationId) {
        this.lastOperationId = lastOperationId;
    }

    public String getStandardLogPath() {
        return standardLogPath;
    }

    public void setStandardLogPath(String standardLogPath) {
        this.standardLogPath = standardLogPath;
    }

    public String getErrorLogPath() {
        return errorLogPath;
    }

    public void setErrorLogPath(String errorLogPath) {
        this.errorLogPath = errorLogPath;
    }

    public String getPidFilePath() {
        return pidFilePath;
    }

    public void setPidFilePath(String pidFilePath) {
        this.pidFilePath = pidFilePath;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
