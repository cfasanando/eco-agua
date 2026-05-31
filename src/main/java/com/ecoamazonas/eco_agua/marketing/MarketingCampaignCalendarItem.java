package com.ecoamazonas.eco_agua.marketing;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.promotion.Promotion;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketing_campaign_calendar")
public class MarketingCampaignCalendarItem {

    public enum CampaignType {
        SEASONAL,
        PRODUCT,
        PROMOTION,
        CONTENT,
        SOCIAL_MEDIA,
        WHATSAPP,
        PUBLIC_PORTAL,
        OTHER
    }

    public enum Status {
        PLANNED,
        PREPARING,
        ACTIVE,
        FINISHED,
        ARCHIVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CampaignType type = CampaignType.SEASONAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.PLANNED;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 160)
    private String channel;

    @Column(name = "target_segment", length = 220)
    private String targetSegment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id")
    private MarketingStrategy strategy;

    @Column(length = 255)
    private String objective;

    @Column(name = "main_message", columnDefinition = "TEXT")
    private String mainMessage;

    @Column(name = "next_action", length = 255)
    private String nextAction;

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
        if (type == null) {
            type = CampaignType.SEASONAL;
        }
        if (status == null) {
            status = Status.PLANNED;
        }
    }


    public String getTypeLabel() {
        if (type == null) {
            return "Temporada";
        }
        return switch (type) {
            case SEASONAL -> "Temporada";
            case PRODUCT -> "Producto";
            case PROMOTION -> "Promoción";
            case CONTENT -> "Contenido";
            case SOCIAL_MEDIA -> "Redes sociales";
            case WHATSAPP -> "WhatsApp";
            case PUBLIC_PORTAL -> "Portal público";
            case OTHER -> "Otro";
        };
    }

    public String getStatusLabel() {
        if (status == null) {
            return "Planificada";
        }
        return switch (status) {
            case PLANNED -> "Planificada";
            case PREPARING -> "En preparación";
            case ACTIVE -> "Activa";
            case FINISHED -> "Finalizada";
            case ARCHIVED -> "Archivada";
        };
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CampaignType getType() {
        return type;
    }

    public void setType(CampaignType type) {
        this.type = type;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTargetSegment() {
        return targetSegment;
    }

    public void setTargetSegment(String targetSegment) {
        this.targetSegment = targetSegment;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
    }

    public MarketingStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(MarketingStrategy strategy) {
        this.strategy = strategy;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getMainMessage() {
        return mainMessage;
    }

    public void setMainMessage(String mainMessage) {
        this.mainMessage = mainMessage;
    }

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
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
