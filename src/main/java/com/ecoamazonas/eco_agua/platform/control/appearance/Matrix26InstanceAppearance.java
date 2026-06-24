package com.ecoamazonas.eco_agua.platform.control.appearance;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "matrix26_instance_appearance")
public class Matrix26InstanceAppearance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instance_id", nullable = false, unique = true)
    private PlatformBusinessClient instance;

    @Column(name = "public_theme_code", nullable = false, length = 80)
    private String publicThemeCode;

    @Column(name = "public_layout_code", nullable = false, length = 80)
    private String publicLayoutCode;

    @Column(name = "admin_theme_code", nullable = false, length = 80)
    private String adminThemeCode;

    @Column(name = "admin_layout_code", nullable = false, length = 80)
    private String adminLayoutCode;

    @Column(name = "login_layout_code", nullable = false, length = 80)
    private String loginLayoutCode;

    @Column(name = "overrides_json", columnDefinition = "TEXT")
    private String overridesJson;

    @Column(nullable = false, length = 30)
    private String status = "PUBLISHED";

    @Column(name = "published_version", nullable = false)
    private int publishedVersion = 1;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by", length = 120)
    private String publishedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (publishedAt == null) {
            publishedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public PlatformBusinessClient getInstance() { return instance; }
    public void setInstance(PlatformBusinessClient instance) { this.instance = instance; }
    public String getPublicThemeCode() { return publicThemeCode; }
    public void setPublicThemeCode(String publicThemeCode) { this.publicThemeCode = publicThemeCode; }
    public String getPublicLayoutCode() { return publicLayoutCode; }
    public void setPublicLayoutCode(String publicLayoutCode) { this.publicLayoutCode = publicLayoutCode; }
    public String getAdminThemeCode() { return adminThemeCode; }
    public void setAdminThemeCode(String adminThemeCode) { this.adminThemeCode = adminThemeCode; }
    public String getAdminLayoutCode() { return adminLayoutCode; }
    public void setAdminLayoutCode(String adminLayoutCode) { this.adminLayoutCode = adminLayoutCode; }
    public String getLoginLayoutCode() { return loginLayoutCode; }
    public void setLoginLayoutCode(String loginLayoutCode) { this.loginLayoutCode = loginLayoutCode; }
    public String getOverridesJson() { return overridesJson; }
    public void setOverridesJson(String overridesJson) { this.overridesJson = overridesJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getPublishedVersion() { return publishedVersion; }
    public void setPublishedVersion(int publishedVersion) { this.publishedVersion = publishedVersion; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String publishedBy) { this.publishedBy = publishedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
