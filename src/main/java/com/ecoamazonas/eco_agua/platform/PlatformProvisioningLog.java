package com.ecoamazonas.eco_agua.platform;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_provisioning_log")
public class PlatformProvisioningLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private PlatformBusinessClient client;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "sql_snippet", columnDefinition = "TEXT")
    private String sqlSnippet;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public PlatformBusinessClient getClient() {
        return client;
    }

    public void setClient(PlatformBusinessClient client) {
        this.client = client;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getSqlSnippet() {
        return sqlSnippet;
    }

    public void setSqlSnippet(String sqlSnippet) {
        this.sqlSnippet = sqlSnippet;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
