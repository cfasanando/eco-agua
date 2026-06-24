package com.ecoamazonas.eco_agua.platform.control.appearance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "matrix26_theme_catalog")
public class Matrix26ThemeCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 140)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 30)
    private String version;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "preview_style", length = 80)
    private String previewStyle;

    @Column(name = "supports_public", nullable = false)
    private boolean supportsPublic;

    @Column(name = "supports_admin", nullable = false)
    private boolean supportsAdmin;

    @Column(name = "default_public_layout_code", length = 80)
    private String defaultPublicLayoutCode;

    @Column(name = "default_admin_layout_code", length = 80)
    private String defaultAdminLayoutCode;

    @Column(name = "tokens_json", columnDefinition = "TEXT")
    private String tokensJson;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 100;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPreviewStyle() { return previewStyle; }
    public void setPreviewStyle(String previewStyle) { this.previewStyle = previewStyle; }
    public boolean isSupportsPublic() { return supportsPublic; }
    public void setSupportsPublic(boolean supportsPublic) { this.supportsPublic = supportsPublic; }
    public boolean isSupportsAdmin() { return supportsAdmin; }
    public void setSupportsAdmin(boolean supportsAdmin) { this.supportsAdmin = supportsAdmin; }
    public String getDefaultPublicLayoutCode() { return defaultPublicLayoutCode; }
    public void setDefaultPublicLayoutCode(String defaultPublicLayoutCode) { this.defaultPublicLayoutCode = defaultPublicLayoutCode; }
    public String getDefaultAdminLayoutCode() { return defaultAdminLayoutCode; }
    public void setDefaultAdminLayoutCode(String defaultAdminLayoutCode) { this.defaultAdminLayoutCode = defaultAdminLayoutCode; }
    public String getTokensJson() { return tokensJson; }
    public void setTokensJson(String tokensJson) { this.tokensJson = tokensJson; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
