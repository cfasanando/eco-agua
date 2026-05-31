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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketing_action_report")
public class MarketingActionReportItem {

    public enum ActionType {
        SOCIAL_POST("Publicación en redes"),
        SHORT_VIDEO("Video corto"),
        WHATSAPP_MESSAGE("Mensaje WhatsApp"),
        BLOG_UPDATE("Actualización de blog"),
        PROMOTION_UPDATE("Actualización de promoción"),
        PUBLIC_CATALOG_UPDATE("Actualización de catálogo"),
        PRODUCT_FEATURED("Producto destacado"),
        BANNER_DESIGN("Diseño de banner"),
        CUSTOMER_REPLY("Respuesta a cliente"),
        OTHER("Otro");

        private final String label;

        ActionType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum Channel {
        TIKTOK_REELS("TikTok / Reels"),
        FACEBOOK("Facebook"),
        WHATSAPP("WhatsApp"),
        BLOG("Blog"),
        PUBLIC_CATALOG("Catálogo público"),
        PUBLIC_PORTAL("Portal público"),
        INTERNAL("Interno"),
        OTHER("Otro");

        private final String label;

        Channel(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum Status {
        REGISTERED("Registrada"),
        FOLLOW_UP("En seguimiento"),
        WITH_RESULT("Con resultado"),
        WITHOUT_RESULT("Sin resultado"),
        ARCHIVED("Archivada");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_date")
    private LocalDate actionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private ActionType actionType = ActionType.SOCIAL_POST;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Channel channel = Channel.TIKTOK_REELS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.REGISTERED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private MarketingCampaignCalendarItem campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_plan_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private MarketingPublicationPlanItem publicationPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idea_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private MarketingContentIdea idea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private Product product;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "observed_result", columnDefinition = "TEXT")
    private String observedResult;

    @Column(name = "generated_inquiries", nullable = false)
    private Integer generatedInquiries = 0;

    @Column(name = "estimated_sales", precision = 12, scale = 2, nullable = false)
    private BigDecimal estimatedSales = BigDecimal.ZERO;

    @Column(length = 120)
    private String responsible;

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
        if (actionType == null) {
            actionType = ActionType.SOCIAL_POST;
        }
        if (channel == null) {
            channel = Channel.TIKTOK_REELS;
        }
        if (status == null) {
            status = Status.REGISTERED;
        }
        if (generatedInquiries == null || generatedInquiries < 0) {
            generatedInquiries = 0;
        }
        if (estimatedSales == null || estimatedSales.compareTo(BigDecimal.ZERO) < 0) {
            estimatedSales = BigDecimal.ZERO;
        }
    }

    public String getActionTypeLabel() {
        return actionType != null ? actionType.getLabel() : ActionType.SOCIAL_POST.getLabel();
    }

    public String getChannelLabel() {
        return channel != null ? channel.getLabel() : Channel.TIKTOK_REELS.getLabel();
    }

    public String getStatusLabel() {
        return status != null ? status.getLabel() : Status.REGISTERED.getLabel();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getActionDate() {
        return actionDate;
    }

    public void setActionDate(LocalDate actionDate) {
        this.actionDate = actionDate;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public MarketingCampaignCalendarItem getCampaign() {
        return campaign;
    }

    public void setCampaign(MarketingCampaignCalendarItem campaign) {
        this.campaign = campaign;
    }

    public MarketingPublicationPlanItem getPublicationPlan() {
        return publicationPlan;
    }

    public void setPublicationPlan(MarketingPublicationPlanItem publicationPlan) {
        this.publicationPlan = publicationPlan;
    }

    public MarketingContentIdea getIdea() {
        return idea;
    }

    public void setIdea(MarketingContentIdea idea) {
        this.idea = idea;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getObservedResult() {
        return observedResult;
    }

    public void setObservedResult(String observedResult) {
        this.observedResult = observedResult;
    }

    public Integer getGeneratedInquiries() {
        return generatedInquiries;
    }

    public void setGeneratedInquiries(Integer generatedInquiries) {
        this.generatedInquiries = generatedInquiries;
    }

    public BigDecimal getEstimatedSales() {
        return estimatedSales;
    }

    public void setEstimatedSales(BigDecimal estimatedSales) {
        this.estimatedSales = estimatedSales;
    }

    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
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
