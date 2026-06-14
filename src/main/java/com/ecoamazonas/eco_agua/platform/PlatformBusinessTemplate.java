package com.ecoamazonas.eco_agua.platform;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_business_template")
public class PlatformBusinessTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(length = 100)
    private String segment;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "default_city", length = 120)
    private String defaultCity;

    @Column(name = "default_currency", length = 10)
    private String defaultCurrency = "PEN";

    @Column(name = "default_primary_color", length = 30)
    private String defaultPrimaryColor;

    @Column(name = "demo_available")
    private boolean demoAvailable = true;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultCity() {
        return defaultCity;
    }

    public void setDefaultCity(String defaultCity) {
        this.defaultCity = defaultCity;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public String getDefaultPrimaryColor() {
        return defaultPrimaryColor;
    }

    public void setDefaultPrimaryColor(String defaultPrimaryColor) {
        this.defaultPrimaryColor = defaultPrimaryColor;
    }

    public boolean isDemoAvailable() {
        return demoAvailable;
    }

    public void setDemoAvailable(boolean demoAvailable) {
        this.demoAvailable = demoAvailable;
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
