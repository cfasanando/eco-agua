package com.ecoamazonas.eco_agua.platform.control.modules;

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
        name = "matrix26_instance_module_activation_event",
        indexes = {
                @Index(name = "idx_matrix26_module_event_instance_created", columnList = "instance_id, created_at"),
                @Index(name = "idx_matrix26_module_event_module_created", columnList = "module_key, created_at"),
                @Index(name = "idx_matrix26_module_event_created", columnList = "created_at")
        }
)
public class Matrix26ModuleActivationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "instance_id")
    private PlatformBusinessClient instance;

    @Column(name = "module_key", nullable = false, length = 80)
    private String moduleKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Matrix26ModuleActivationAction action;

    @Column(name = "before_enabled", nullable = false)
    private boolean beforeEnabled;

    @Column(name = "after_enabled", nullable = false)
    private boolean afterEnabled;

    @Column(name = "actor_username", nullable = false, length = 120)
    private String actorUsername;

    @Column(name = "source", nullable = false, length = 80)
    private String source;

    @Column(length = 500)
    private String notes;

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

    public PlatformBusinessClient getInstance() {
        return instance;
    }

    public void setInstance(PlatformBusinessClient instance) {
        this.instance = instance;
    }

    public String getModuleKey() {
        return moduleKey;
    }

    public void setModuleKey(String moduleKey) {
        this.moduleKey = moduleKey;
    }

    public Matrix26ModuleActivationAction getAction() {
        return action;
    }

    public void setAction(Matrix26ModuleActivationAction action) {
        this.action = action;
    }

    public boolean isBeforeEnabled() {
        return beforeEnabled;
    }

    public void setBeforeEnabled(boolean beforeEnabled) {
        this.beforeEnabled = beforeEnabled;
    }

    public boolean isAfterEnabled() {
        return afterEnabled;
    }

    public void setAfterEnabled(boolean afterEnabled) {
        this.afterEnabled = afterEnabled;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
