package com.ecoamazonas.eco_agua.platform;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_module_catalog")
public class PlatformModuleCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_key", nullable = false, unique = true, length = 80)
    private String moduleKey;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 120)
    private String area;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "default_enabled")
    private boolean defaultEnabled;

    @Column(nullable = false)
    private boolean configurable = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "display_order")
    private int displayOrder = 100;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
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

    public String getModuleKey() {
        return moduleKey;
    }

    public void setModuleKey(String moduleKey) {
        this.moduleKey = moduleKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public void setDefaultEnabled(boolean defaultEnabled) {
        this.defaultEnabled = defaultEnabled;
    }

    public boolean isConfigurable() {
        return configurable;
    }

    public void setConfigurable(boolean configurable) {
        this.configurable = configurable;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
