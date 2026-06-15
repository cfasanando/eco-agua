package com.ecoamazonas.eco_agua.platform;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_business_client")
public class PlatformBusinessClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(name = "business_name", nullable = false, length = 160)
    private String businessName;

    @Column(name = "legal_name", length = 180)
    private String legalName;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private PlatformBusinessTemplate template;

    @Column(name = "database_name", length = 120)
    private String databaseName;

    @Column(name = "database_status", length = 50)
    private String databaseStatus = "PENDING";

    @Column(length = 50)
    private String status = "DRAFT";

    @Column(name = "owner_name", length = 150)
    private String ownerName;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(length = 120)
    private String city;

    @Column(length = 10)
    private String currency = "PEN";

    @Column(length = 50)
    private String whatsapp;

    @Column(name = "primary_color", length = 30)
    private String primaryColor;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "public_slug", length = 120)
    private String publicSlug;

    @Column(name = "demo_data_enabled")
    private boolean demoDataEnabled;

    @Column(name = "runtime_profile", length = 120)
    private String runtimeProfile;

    @Column(name = "runtime_port")
    private Integer runtimePort;

    @Column(name = "public_url", length = 500)
    private String publicUrl;

    @Column(name = "runtime_status", length = 50)
    private String runtimeStatus = "PENDING";

    @Column(name = "management_mode", length = 50)
    private String managementMode = "DEMO";

    @Column(name = "monitor_visible")
    private boolean monitorVisible = true;

    @Column(name = "protected_instance")
    private boolean protectedInstance;

    @Column(name = "runtime_command", length = 500)
    private String runtimeCommand;

    @Column(name = "last_health_status", length = 50)
    private String lastHealthStatus;

    @Column(name = "last_health_checked_at")
    private LocalDateTime lastHealthCheckedAt;

    @Column(name = "last_health_message", length = 500)
    private String lastHealthMessage;

    @Column(name = "last_runtime_generated_at")
    private LocalDateTime lastRuntimeGeneratedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

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

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public PlatformBusinessTemplate getTemplate() {
        return template;
    }

    public void setTemplate(PlatformBusinessTemplate template) {
        this.template = template;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabaseStatus() {
        return databaseStatus;
    }

    public void setDatabaseStatus(String databaseStatus) {
        this.databaseStatus = databaseStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getPublicSlug() {
        return publicSlug;
    }

    public void setPublicSlug(String publicSlug) {
        this.publicSlug = publicSlug;
    }


    public String getRuntimeProfile() {
        return runtimeProfile;
    }

    public void setRuntimeProfile(String runtimeProfile) {
        this.runtimeProfile = runtimeProfile;
    }

    public Integer getRuntimePort() {
        return runtimePort;
    }

    public void setRuntimePort(Integer runtimePort) {
        this.runtimePort = runtimePort;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public String getRuntimeStatus() {
        return runtimeStatus;
    }

    public void setRuntimeStatus(String runtimeStatus) {
        this.runtimeStatus = runtimeStatus;
    }

    public String getManagementMode() {
        return managementMode;
    }

    public void setManagementMode(String managementMode) {
        this.managementMode = managementMode;
    }

    public boolean isMonitorVisible() {
        return monitorVisible;
    }

    public void setMonitorVisible(boolean monitorVisible) {
        this.monitorVisible = monitorVisible;
    }

    public boolean isProtectedInstance() {
        return protectedInstance;
    }

    public void setProtectedInstance(boolean protectedInstance) {
        this.protectedInstance = protectedInstance;
    }

    public String getRuntimeCommand() {
        return runtimeCommand;
    }

    public void setRuntimeCommand(String runtimeCommand) {
        this.runtimeCommand = runtimeCommand;
    }

    public String getLastHealthStatus() {
        return lastHealthStatus;
    }

    public void setLastHealthStatus(String lastHealthStatus) {
        this.lastHealthStatus = lastHealthStatus;
    }

    public LocalDateTime getLastHealthCheckedAt() {
        return lastHealthCheckedAt;
    }

    public void setLastHealthCheckedAt(LocalDateTime lastHealthCheckedAt) {
        this.lastHealthCheckedAt = lastHealthCheckedAt;
    }

    public String getLastHealthMessage() {
        return lastHealthMessage;
    }

    public void setLastHealthMessage(String lastHealthMessage) {
        this.lastHealthMessage = lastHealthMessage;
    }

    public LocalDateTime getLastRuntimeGeneratedAt() {
        return lastRuntimeGeneratedAt;
    }

    public void setLastRuntimeGeneratedAt(LocalDateTime lastRuntimeGeneratedAt) {
        this.lastRuntimeGeneratedAt = lastRuntimeGeneratedAt;
    }

    public boolean isDemoDataEnabled() {
        return demoDataEnabled;
    }

    public void setDemoDataEnabled(boolean demoDataEnabled) {
        this.demoDataEnabled = demoDataEnabled;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
