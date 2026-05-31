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
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "marketing_image_asset")
public class MarketingImageAsset {

    public enum AssetType {
        PRODUCT_PHOTO("Foto de producto"),
        BANNER("Banner"),
        BLOG_COVER("Portada de blog"),
        SOCIAL_MEDIA("Imagen para redes"),
        CAMPAIGN_RESOURCE("Recurso de campaña"),
        REFERENCE("Referencia visual"),
        OTHER("Otro");

        private final String label;

        AssetType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum RecommendedChannel {
        FACEBOOK("Facebook"),
        TIKTOK("TikTok"),
        WHATSAPP("WhatsApp"),
        BLOG("Blog"),
        PUBLIC_PORTAL("Portal público"),
        CATALOG("Catálogo"),
        ALL("Todos los canales"),
        INTERNAL("Uso interno");

        private final String label;

        RecommendedChannel(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum Status {
        ACTIVE("Activo"),
        ARCHIVED("Archivado");

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

    @Column(nullable = false, length = 180)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_path", nullable = false, length = 255)
    private String imagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 40)
    private AssetType assetType = AssetType.PRODUCT_PHOTO;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_channel", nullable = false, length = 40)
    private RecommendedChannel recommendedChannel = RecommendedChannel.ALL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private MarketingCampaignCalendarItem campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private Promotion promotion;

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
        if (assetType == null) {
            assetType = AssetType.PRODUCT_PHOTO;
        }
        if (recommendedChannel == null) {
            recommendedChannel = RecommendedChannel.ALL;
        }
        if (status == null) {
            status = Status.ACTIVE;
        }
    }

    public String getAssetTypeLabel() {
        return assetType != null ? assetType.getLabel() : AssetType.PRODUCT_PHOTO.getLabel();
    }

    public String getRecommendedChannelLabel() {
        return recommendedChannel != null ? recommendedChannel.getLabel() : RecommendedChannel.ALL.getLabel();
    }

    public String getStatusLabel() {
        return status != null ? status.getLabel() : Status.ACTIVE.getLabel();
    }

    public String getTitleDisplay() {
        return isBlank(title) ? "Recurso visual" : title;
    }

    public String getDescriptionPreview() {
        return preview(description, 120, "Sin descripción registrada.");
    }

    public String getObservationsPreview() {
        return preview(observations, 120, null);
    }

    private String preview(String value, int maxLength, String fallback) {
        if (isBlank(value)) {
            return fallback;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public RecommendedChannel getRecommendedChannel() {
        return recommendedChannel;
    }

    public void setRecommendedChannel(RecommendedChannel recommendedChannel) {
        this.recommendedChannel = recommendedChannel;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public Promotion getPromotion() {
        return promotion;
    }

    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
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
