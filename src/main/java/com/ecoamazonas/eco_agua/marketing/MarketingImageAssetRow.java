package com.ecoamazonas.eco_agua.marketing;

import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class MarketingImageAssetRow {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Long id;
    private final String title;
    private final String descriptionPreview;
    private final String imagePath;
    private final String assetTypeLabel;
    private final String recommendedChannelLabel;
    private final String statusLabel;
    private final String statusBadgeClass;
    private final String relationText;
    private final String observationsPreview;
    private final String updatedAtText;
    private final boolean active;
    private final boolean archived;

    private MarketingImageAssetRow(Long id,
                                   String title,
                                   String descriptionPreview,
                                   String imagePath,
                                   String assetTypeLabel,
                                   String recommendedChannelLabel,
                                   String statusLabel,
                                   String statusBadgeClass,
                                   String relationText,
                                   String observationsPreview,
                                   String updatedAtText,
                                   boolean active,
                                   boolean archived) {
        this.id = id;
        this.title = title;
        this.descriptionPreview = descriptionPreview;
        this.imagePath = imagePath;
        this.assetTypeLabel = assetTypeLabel;
        this.recommendedChannelLabel = recommendedChannelLabel;
        this.statusLabel = statusLabel;
        this.statusBadgeClass = statusBadgeClass;
        this.relationText = relationText;
        this.observationsPreview = observationsPreview;
        this.updatedAtText = updatedAtText;
        this.active = active;
        this.archived = archived;
    }

    public static MarketingImageAssetRow from(MarketingImageAsset asset) {
        MarketingImageAsset.Status status = asset.getStatus() != null ? asset.getStatus() : MarketingImageAsset.Status.ACTIVE;
        return new MarketingImageAssetRow(
                asset.getId(),
                asset.getTitleDisplay(),
                asset.getDescriptionPreview(),
                asset.getImagePath(),
                asset.getAssetTypeLabel(),
                asset.getRecommendedChannelLabel(),
                asset.getStatusLabel(),
                badgeClass(status),
                relationText(asset),
                asset.getObservationsPreview(),
                asset.getUpdatedAt() != null ? asset.getUpdatedAt().format(DATE_TIME_FORMATTER) : "Sin actualización",
                status == MarketingImageAsset.Status.ACTIVE,
                status == MarketingImageAsset.Status.ARCHIVED
        );
    }

    private static String badgeClass(MarketingImageAsset.Status status) {
        if (status == MarketingImageAsset.Status.ARCHIVED) {
            return "text-bg-secondary";
        }
        return "text-bg-success";
    }

    private static String relationText(MarketingImageAsset asset) {
        String productName = safe(() -> asset.getProduct() != null ? asset.getProduct().getName() : null);
        if (productName != null) {
            return "Producto: " + productName;
        }

        String campaignName = safe(() -> asset.getCampaign() != null ? asset.getCampaign().getName() : null);
        if (campaignName != null) {
            return "Campaña: " + campaignName;
        }

        String promotionName = safe(() -> asset.getPromotion() != null ? asset.getPromotion().getName() : null);
        if (promotionName != null) {
            return "Promoción: " + promotionName;
        }

        return "Sin relación";
    }

    private static String safe(Supplier<String> supplier) {
        try {
            return trimToNull(supplier.get());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescriptionPreview() {
        return descriptionPreview;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getAssetTypeLabel() {
        return assetTypeLabel;
    }

    public String getRecommendedChannelLabel() {
        return recommendedChannelLabel;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getStatusBadgeClass() {
        return statusBadgeClass;
    }

    public String getRelationText() {
        return relationText;
    }

    public String getObservationsPreview() {
        return observationsPreview;
    }

    public String getUpdatedAtText() {
        return updatedAtText;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isArchived() {
        return archived;
    }
}
