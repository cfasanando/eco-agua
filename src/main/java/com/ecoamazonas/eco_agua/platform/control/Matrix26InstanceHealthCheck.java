package com.ecoamazonas.eco_agua.platform.control;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
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
        name = "matrix26_instance_health_check",
        indexes = {
                @Index(name = "idx_matrix26_health_instance_checked", columnList = "instance_id, checked_at"),
                @Index(name = "idx_matrix26_health_checked", columnList = "checked_at")
        }
)
public class Matrix26InstanceHealthCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "instance_id", nullable = false)
    private PlatformBusinessClient instance;

    @Column(nullable = false)
    private boolean online;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(length = 500)
    private String message;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @PrePersist
    void onCreate() {
        if (checkedAt == null) {
            checkedAt = LocalDateTime.now();
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

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }
}
