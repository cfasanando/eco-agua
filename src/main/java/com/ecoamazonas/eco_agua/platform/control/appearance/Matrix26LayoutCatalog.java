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
@Table(name = "matrix26_layout_catalog")
public class Matrix26LayoutCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String area;

    @Column(nullable = false, length = 30)
    private String version;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "preview_style", length = 80)
    private String previewStyle;

    @Column(name = "compatible_themes", length = 500)
    private String compatibleThemes;

    @Column(name = "configuration_json", columnDefinition = "TEXT")
    private String configurationJson;

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
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPreviewStyle() { return previewStyle; }
    public void setPreviewStyle(String previewStyle) { this.previewStyle = previewStyle; }
    public String getCompatibleThemes() { return compatibleThemes; }
    public void setCompatibleThemes(String compatibleThemes) { this.compatibleThemes = compatibleThemes; }
    public String getConfigurationJson() { return configurationJson; }
    public void setConfigurationJson(String configurationJson) { this.configurationJson = configurationJson; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
