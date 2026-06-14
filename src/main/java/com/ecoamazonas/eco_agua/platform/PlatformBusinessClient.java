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
