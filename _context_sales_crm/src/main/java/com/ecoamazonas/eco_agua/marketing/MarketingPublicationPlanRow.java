package com.ecoamazonas.eco_agua.marketing;

import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class MarketingPublicationPlanRow {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Long id;
    private final String title;
    private final String publicationDateText;
    private final String baseTextPreview;
    private final String ideaTitle;
    private final String campaignName;
    private final String productName;
    private final String strategyTitle;
    private final String channelLabel;
    private final String publicationTypeLabel;
    private final String statusLabel;
    private final String statusBadgeClass;
    private final String resultNoteDisplay;
    private final String responsibleDisplay;
    private final boolean canPrepare;
    private final boolean canMarkReady;
    private final boolean canPublish;
    private final boolean canCancel;

    private MarketingPublicationPlanRow(Long id,
                                        String title,
                                        String publicationDateText,
                                        String baseTextPreview,
                                        String ideaTitle,
                                        String campaignName,
                                        String productName,
                                        String strategyTitle,
                                        String channelLabel,
                                        String publicationTypeLabel,
                                        String statusLabel,
                                        String statusBadgeClass,
                                        String resultNoteDisplay,
                                        String responsibleDisplay,
                                        boolean canPrepare,
                                        boolean canMarkReady,
                                        boolean canPublish,
                                        boolean canCancel) {
        this.id = id;
        this.title = title;
        this.publicationDateText = publicationDateText;
        this.baseTextPreview = baseTextPreview;
        this.ideaTitle = ideaTitle;
        this.campaignName = campaignName;
        this.productName = productName;
        this.strategyTitle = strategyTitle;
        this.channelLabel = channelLabel;
        this.publicationTypeLabel = publicationTypeLabel;
        this.statusLabel = statusLabel;
        this.statusBadgeClass = statusBadgeClass;
        this.resultNoteDisplay = resultNoteDisplay;
        this.responsibleDisplay = responsibleDisplay;
        this.canPrepare = canPrepare;
        this.canMarkReady = canMarkReady;
        this.canPublish = canPublish;
        this.canCancel = canCancel;
    }

    public static MarketingPublicationPlanRow from(MarketingPublicationPlanItem item) {
        MarketingPublicationPlanItem.Status status = item.getStatus();
        return new MarketingPublicationPlanRow(
                item.getId(),
                defaultText(item.getTitle(), "Publicación de marketing"),
                item.getPublicationDate() != null ? item.getPublicationDate().format(DATE_FORMATTER) : null,
                preview(item.getBaseText(), 110),
                safe(() -> item.getIdea() != null ? item.getIdea().getTitle() : null),
                safe(() -> item.getCampaign() != null ? item.getCampaign().getName() : null),
                safe(() -> item.getProduct() != null ? item.getProduct().getName() : null),
                safe(() -> item.getStrategy() != null ? item.getStrategy().getTitle() : null),
                item.getChannelLabel(),
                item.getPublicationTypeLabel(),
                item.getStatusLabel(),
                badgeClass(status),
                defaultText(item.getResultNote(), "Sin resultado registrado."),
                defaultText(item.getResponsible(), "Marketing"),
                status == null || status == MarketingPublicationPlanItem.Status.PENDING,
                status != MarketingPublicationPlanItem.Status.READY
                        && status != MarketingPublicationPlanItem.Status.PUBLISHED
                        && status != MarketingPublicationPlanItem.Status.CANCELLED,
                status != MarketingPublicationPlanItem.Status.PUBLISHED
                        && status != MarketingPublicationPlanItem.Status.CANCELLED,
                status == null || status != MarketingPublicationPlanItem.Status.CANCELLED
        );
    }

    private static String safe(Supplier<String> supplier) {
        try {
            return trimToNull(supplier.get());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String badgeClass(MarketingPublicationPlanItem.Status status) {
        if (status == MarketingPublicationPlanItem.Status.PUBLISHED) {
            return "text-bg-success";
        }
        if (status == MarketingPublicationPlanItem.Status.READY) {
            return "text-bg-primary";
        }
        if (status == MarketingPublicationPlanItem.Status.PREPARING) {
            return "text-bg-warning";
        }
        if (status == MarketingPublicationPlanItem.Status.CANCELLED) {
            return "text-bg-secondary";
        }
        return "text-bg-info";
    }

    private static String preview(String value, int maxLength) {
        String trimmedValue = trimToNull(value);
        if (trimmedValue == null) {
            return null;
        }
        if (trimmedValue.length() <= maxLength) {
            return trimmedValue;
        }
        return trimmedValue.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static String defaultText(String value, String defaultValue) {
        String trimmedValue = trimToNull(value);
        return trimmedValue != null ? trimmedValue : defaultValue;
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

    public String getPublicationDateText() {
        return publicationDateText;
    }

    public String getBaseTextPreview() {
        return baseTextPreview;
    }

    public String getIdeaTitle() {
        return ideaTitle;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public String getProductName() {
        return productName;
    }

    public String getStrategyTitle() {
        return strategyTitle;
    }

    public String getChannelLabel() {
        return channelLabel;
    }

    public String getPublicationTypeLabel() {
        return publicationTypeLabel;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getStatusBadgeClass() {
        return statusBadgeClass;
    }

    public String getResultNoteDisplay() {
        return resultNoteDisplay;
    }

    public String getResponsibleDisplay() {
        return responsibleDisplay;
    }

    public boolean isCanPrepare() {
        return canPrepare;
    }

    public boolean isCanMarkReady() {
        return canMarkReady;
    }

    public boolean isCanPublish() {
        return canPublish;
    }

    public boolean isCanCancel() {
        return canCancel;
    }
}
