package com.ecoamazonas.eco_agua.platform;

import jakarta.persistence.*;

@Entity
@Table(
        name = "platform_client_module",
        uniqueConstraints = @UniqueConstraint(name = "uk_platform_client_module", columnNames = {"client_id", "module_id"})
)
public class PlatformClientModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private PlatformBusinessClient client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private PlatformModuleCatalog module;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "selection_source", length = 50)
    private String selectionSource = "MANUAL";

    @Column(length = 255)
    private String notes;

    public Long getId() {
        return id;
    }

    public PlatformBusinessClient getClient() {
        return client;
    }

    public void setClient(PlatformBusinessClient client) {
        this.client = client;
    }

    public PlatformModuleCatalog getModule() {
        return module;
    }

    public void setModule(PlatformModuleCatalog module) {
        this.module = module;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSelectionSource() {
        return selectionSource;
    }

    public void setSelectionSource(String selectionSource) {
        this.selectionSource = selectionSource;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
