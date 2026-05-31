package com.ecoamazonas.eco_agua.marketing;

import com.ecoamazonas.eco_agua.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketing_featured_product")
public class MarketingFeaturedProduct {

    public enum Status {
        PLANNED("Planificado"),
        ACTIVE("Activo"),
        PAUSED("Pausado"),
        FINISHED("Finalizado"),
        ARCHIVED("Archivado");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum DisplayPlace {
        HOME("Inicio"),
        CATALOG("Catálogo"),
        SOCIAL_MEDIA("Redes sociales"),
        WHATSAPP("WhatsApp"),
        ALL("Todos los canales");

        private final String label;

        DisplayPlace(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Product product;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "short_text", columnDefinition = "TEXT")
    private String shortText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.PLANNED;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_place", nullable = false, length = 30)
    private DisplayPlace displayPlace = DisplayPlace.HOME;

    @Column(nullable = false)
    private Integer priority = 1;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "call_to_action", length = 180)
    private String callToAction;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        normalizeDefaults();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalizeDefaults();
    }

    private void normalizeDefaults() {
        if (status == null) {
            status = Status.PLANNED;
        }
        if (displayPlace == null) {
            displayPlace = DisplayPlace.HOME;
        }
        if (priority == null || priority < 1) {
            priority = 1;
        }
        if (priority > 99) {
            priority = 99;
        }
    }

    public String getStatusLabel() {
        return status != null ? status.getLabel() : Status.PLANNED.getLabel();
    }

    public String getDisplayPlaceLabel() {
        return displayPlace != null ? displayPlace.getLabel() : DisplayPlace.HOME.getLabel();
    }

    public String getProductName() {
        return product != null ? product.getName() : null;
    }

    public String getTitleDisplay() {
        return isBlank(title) ? "Producto destacado" : title;
    }

    public String getCallToActionDisplay() {
        return isBlank(callToAction) ? "Consultar por WhatsApp" : callToAction;
    }

    public String getShortTextPreview() {
        return preview(shortText, 120);
    }

    private String preview(String value, int maxLength) {
        if (isBlank(value)) {
            return null;
        }
        String trimmedValue = value.trim();
        if (trimmedValue.length() <= maxLength) {
            return trimmedValue;
        }
        return trimmedValue.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShortText() {
        return shortText;
    }

    public void setShortText(String shortText) {
        this.shortText = shortText;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public DisplayPlace getDisplayPlace() {
        return displayPlace;
    }

    public void setDisplayPlace(DisplayPlace displayPlace) {
        this.displayPlace = displayPlace;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(String callToAction) {
        this.callToAction = callToAction;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
