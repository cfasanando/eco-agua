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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketing_content_idea")
public class MarketingContentIdea {

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

    public enum ContentType {
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

        ContentType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum Status {
        NEW("Nueva"),
        SELECTED("Seleccionada"),
        PREPARING("En preparación"),
        PUBLISHED("Publicada"),
        DISCARDED("Descartada");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum Priority {
        HIGH("Alta"),
        MEDIUM("Media"),
        LOW("Baja");

        private final String label;

        Priority(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Channel channel = Channel.TIKTOK_REELS;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private ContentType contentType = ContentType.SHORT_VIDEO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "suggested_date")
    private LocalDate suggestedDate;

    @Column(name = "target_segment", length = 220)
    private String targetSegment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private MarketingCampaignCalendarItem campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id")
    private MarketingStrategy strategy;

    @Column(length = 255)
    private String hook;

    @Column(name = "main_message", columnDefinition = "TEXT")
    private String mainMessage;

    @Column(name = "call_to_action", length = 255)
    private String callToAction;

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
        if (channel == null) {
            channel = Channel.TIKTOK_REELS;
        }
        if (contentType == null) {
            contentType = ContentType.SHORT_VIDEO;
        }
        if (status == null) {
            status = Status.NEW;
        }
        if (priority == null) {
            priority = Priority.MEDIUM;
        }
    }

    public String getChannelLabel() {
        return channel != null ? channel.getLabel() : Channel.TIKTOK_REELS.getLabel();
    }

    public String getContentTypeLabel() {
        return contentType != null ? contentType.getLabel() : ContentType.SHORT_VIDEO.getLabel();
    }

    public String getStatusLabel() {
        return status != null ? status.getLabel() : Status.NEW.getLabel();
    }

    public String getPriorityLabel() {
        return priority != null ? priority.getLabel() : Priority.MEDIUM.getLabel();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDate getSuggestedDate() {
        return suggestedDate;
    }

    public void setSuggestedDate(LocalDate suggestedDate) {
        this.suggestedDate = suggestedDate;
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

    public String getHook() {
        return hook;
    }

    public void setHook(String hook) {
        this.hook = hook;
    }

    public String getMainMessage() {
        return mainMessage;
    }

    public void setMainMessage(String mainMessage) {
        this.mainMessage = mainMessage;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(String callToAction) {
        this.callToAction = callToAction;
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
