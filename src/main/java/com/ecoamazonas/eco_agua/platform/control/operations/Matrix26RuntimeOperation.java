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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "matrix26_runtime_operation",
        indexes = {
                @Index(name = "idx_matrix26_runtime_operation_instance", columnList = "instance_id, requested_at"),
                @Index(name = "idx_matrix26_runtime_operation_status", columnList = "status, requested_at")
        }
)
public class Matrix26RuntimeOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instance_id", nullable = false)
    private PlatformBusinessClient instance;

    @Column(name = "runtime_key", nullable = false, length = 80)
    private String runtimeKey;

    @Column(name = "instance_code", nullable = false, length = 80)
    private String instanceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Matrix26RuntimeOperationAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Matrix26RuntimeOperationStatus status;

    @Column(name = "requested_by", nullable = false, length = 120)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "previous_pid")
    private Long previousPid;

    @Column(name = "resulting_pid")
    private Long resultingPid;

    @Column(name = "runtime_port")
    private Integer runtimePort;

    @Column(name = "initial_state", length = 40)
    private String initialState;

    @Column(name = "final_state", length = 40)
    private String finalState;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(length = 500)
    private String message;

    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;

    @Column(name = "standard_log_path", length = 500)
    private String standardLogPath;

    @Column(name = "error_log_path", length = 500)
    private String errorLogPath;

    @PrePersist
    void onCreate() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = Matrix26RuntimeOperationStatus.REQUESTED;
        }
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

    public Matrix26RuntimeOperationAction getAction() {
        return action;
    }

    public void setAction(Matrix26RuntimeOperationAction action) {
        this.action = action;
    }

    public Matrix26RuntimeOperationStatus getStatus() {
        return status;
    }

    public void setStatus(Matrix26RuntimeOperationStatus status) {
        this.status = status;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getPreviousPid() {
        return previousPid;
    }

    public void setPreviousPid(Long previousPid) {
        this.previousPid = previousPid;
    }

    public Long getResultingPid() {
        return resultingPid;
    }

    public void setResultingPid(Long resultingPid) {
        this.resultingPid = resultingPid;
    }

    public Integer getRuntimePort() {
        return runtimePort;
    }

    public void setRuntimePort(Integer runtimePort) {
        this.runtimePort = runtimePort;
    }

    public String getInitialState() {
        return initialState;
    }

    public void setInitialState(String initialState) {
        this.initialState = initialState;
    }

    public String getFinalState() {
        return finalState;
    }

    public void setFinalState(String finalState) {
        this.finalState = finalState;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
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
}
