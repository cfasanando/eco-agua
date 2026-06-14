package com.ecoamazonas.eco_agua.platform;

import jakarta.persistence.*;

@Entity
@Table(
        name = "platform_business_template_module",
        uniqueConstraints = @UniqueConstraint(name = "uk_platform_template_module", columnNames = {"template_id", "module_id"})
)
public class PlatformBusinessTemplateModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private PlatformBusinessTemplate template;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private PlatformModuleCatalog module;

    @Column(nullable = false)
    private boolean recommended = true;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "display_order")
    private int displayOrder = 100;

    @Column(length = 255)
    private String notes;

    public Long getId() {
        return id;
    }

    public PlatformBusinessTemplate getTemplate() {
        return template;
    }

    public void setTemplate(PlatformBusinessTemplate template) {
        this.template = template;
    }

    public PlatformModuleCatalog getModule() {
        return module;
    }

    public void setModule(PlatformModuleCatalog module) {
        this.module = module;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
