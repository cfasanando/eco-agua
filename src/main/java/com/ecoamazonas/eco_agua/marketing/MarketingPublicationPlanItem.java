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
@Table(name = "marketing_publication_plan")
public class MarketingPublicationPlanItem {

    public enum Channel {
        TIKTOK_REELS("TikTok / Reels"),
        FACEBOOK("Facebook"),
        WHATSAPP("WhatsApp"),
        BLOG("Blog"),
        PUBLIC_CATALOG("Catálogo público"),
        PUBLIC_PORTAL("Portal público"),
        OTHER("Otro");

        private final String label;

        Channel(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum PublicationType {
        SHORT_VIDEO("Video corto"),
        IMAGE_POST("Imagen / carrusel"),
        STORY("Historia"),
        WHATSAPP_MESSAGE("Mensaje WhatsApp"),
        BLOG_ARTICLE("Artículo blog"),
        PRODUCT_PHOTO("Foto de producto"),
        PROMOTION_BANNER("Banner promocional"),
        EDUCATIONAL("Educativo"),
        TESTIMONIAL("Testimonio"),
        OTHER("Otro");

        private final String label;

        PublicationType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum Status {
        PENDING("Pendiente"),
        PREPARING("En preparación"),
        READY("Listo"),
        PUBLISHED("Publicado"),
        CANCELLED("Cancelado");

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

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Channel channel = Channel.TIKTOK_REELS;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_type", nullable = false, length = 30)
    private PublicationType publicationType = PublicationType.SHORT_VIDEO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.PENDING;

    @Column(nullable = false, length = 180)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idea_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private MarketingContentIdea idea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private MarketingCampaignCalendarItem campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private MarketingStrategy strategy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private Product product;

    @Column(name = "base_text", columnDefinition = "TEXT")
    private String baseText;

    @Column(length = 120)
    private String responsible;

    @Column(name = "result_note", columnDefinition = "TEXT")
    private String resultNote;

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
        if (channel == null) {
            channel = Channel.TIKTOK_REELS;
        }
        if (publicationType == null) {
            publicationType = PublicationType.SHORT_VIDEO;
        }
        if (status == null) {
            status = Status.PENDING;
        }
    }

    public String getChannelLabel() {
        return channel != null ? channel.getLabel() : Channel.TIKTOK_REELS.getLabel();
    }

    public String getPublicationTypeLabel() {
        return publicationType != null ? publicationType.getLabel() : PublicationType.SHORT_VIDEO.getLabel();
    }

    public String getStatusLabel() {
        return status != null ? status.getLabel() : Status.PENDING.getLabel();
    }

    public String getBaseTextPreview() {
        return preview(baseText, 110);
    }

    public String getResultNoteDisplay() {
        return isBlank(resultNote) ? "Sin resultado registrado." : resultNote;
    }

    public String getResponsibleDisplay() {
        return isBlank(responsible) ? "Marketing" : responsible;
    }

    public String getIdeaTitle() {
        return idea != null ? idea.getTitle() : null;
    }

    public String getCampaignName() {
        return campaign != null ? campaign.getName() : null;
    }

    public String getStrategyTitle() {
        return strategy != null ? strategy.getTitle() : null;
    }

    public String getProductName() {
        return product != null ? product.getName() : null;
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

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public PublicationType getPublicationType() {
        return publicationType;
    }

    public void setPublicationType(PublicationType publicationType) {
        this.publicationType = publicationType;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public MarketingContentIdea getIdea() {
        return idea;
    }

    public void setIdea(MarketingContentIdea idea) {
        this.idea = idea;
    }

    public MarketingCampaignCalendarItem getCampaign() {
        return campaign;
    }

    public void setCampaign(MarketingCampaignCalendarItem campaign) {
        this.campaign = campaign;
    }

    public MarketingStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(MarketingStrategy strategy) {
        this.strategy = strategy;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getBaseText() {
        return baseText;
    }

    public void setBaseText(String baseText) {
        this.baseText = baseText;
    }

    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
    }

    public String getResultNote() {
        return resultNote;
    }

    public void setResultNote(String resultNote) {
        this.resultNote = resultNote;
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
