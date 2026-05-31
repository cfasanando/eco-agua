package com.ecoamazonas.eco_agua.marketing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class MarketingActionReportRow {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Long id;
    private final String actionDateText;
    private final String actionTypeLabel;
    private final String channelLabel;
    private final String statusLabel;
    private final String statusBadgeClass;
    private final String descriptionPreview;
    private final String observedResultPreview;
    private final String campaignName;
    private final String publicationTitle;
    private final String ideaTitle;
    private final String productName;
    private final String generatedInquiriesText;
    private final String estimatedSalesText;
    private final String responsibleDisplay;
    private final boolean canFollowUp;
    private final boolean canMarkWithResult;
    private final boolean canMarkWithoutResult;
    private final boolean canArchive;

    private MarketingActionReportRow(Long id,
                                     String actionDateText,
                                     String actionTypeLabel,
                                     String channelLabel,
                                     String statusLabel,
                                     String statusBadgeClass,
                                     String descriptionPreview,
                                     String observedResultPreview,
                                     String campaignName,
                                     String publicationTitle,
                                     String ideaTitle,
                                     String productName,
                                     String generatedInquiriesText,
                                     String estimatedSalesText,
                                     String responsibleDisplay,
                                     boolean canFollowUp,
                                     boolean canMarkWithResult,
                                     boolean canMarkWithoutResult,
                                     boolean canArchive) {
        this.id = id;
        this.actionDateText = actionDateText;
        this.actionTypeLabel = actionTypeLabel;
        this.channelLabel = channelLabel;
        this.statusLabel = statusLabel;
        this.statusBadgeClass = statusBadgeClass;
        this.descriptionPreview = descriptionPreview;
        this.observedResultPreview = observedResultPreview;
        this.campaignName = campaignName;
        this.publicationTitle = publicationTitle;
        this.ideaTitle = ideaTitle;
        this.productName = productName;
        this.generatedInquiriesText = generatedInquiriesText;
        this.estimatedSalesText = estimatedSalesText;
        this.responsibleDisplay = responsibleDisplay;
        this.canFollowUp = canFollowUp;
        this.canMarkWithResult = canMarkWithResult;
        this.canMarkWithoutResult = canMarkWithoutResult;
        this.canArchive = canArchive;
    }

    public static MarketingActionReportRow from(MarketingActionReportItem item) {
        MarketingActionReportItem.Status status = item.getStatus();
        return new MarketingActionReportRow(
                item.getId(),
                item.getActionDate() != null ? item.getActionDate().format(DATE_FORMATTER) : null,
                item.getActionTypeLabel(),
                item.getChannelLabel(),
                item.getStatusLabel(),
                badgeClass(status),
                preview(item.getDescription(), 120),
                preview(item.getObservedResult(), 120),
                safe(() -> item.getCampaign() != null ? item.getCampaign().getName() : null),
                safe(() -> item.getPublicationPlan() != null ? item.getPublicationPlan().getTitle() : null),
                safe(() -> item.getIdea() != null ? item.getIdea().getTitle() : null),
                safe(() -> item.getProduct() != null ? item.getProduct().getName() : null),
                String.valueOf(item.getGeneratedInquiries() != null ? item.getGeneratedInquiries() : 0),
                money(item.getEstimatedSales()),
                defaultText(item.getResponsible(), "Marketing"),
                status == null || status == MarketingActionReportItem.Status.REGISTERED,
                status != MarketingActionReportItem.Status.WITH_RESULT
                        && status != MarketingActionReportItem.Status.ARCHIVED,
                status != MarketingActionReportItem.Status.WITHOUT_RESULT
                        && status != MarketingActionReportItem.Status.ARCHIVED,
                status == null || status != MarketingActionReportItem.Status.ARCHIVED
        );
    }

    private static String safe(Supplier<String> supplier) {
        try {
            return trimToNull(supplier.get());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String badgeClass(MarketingActionReportItem.Status status) {
        if (status == MarketingActionReportItem.Status.WITH_RESULT) {
            return "text-bg-success";
        }
        if (status == MarketingActionReportItem.Status.FOLLOW_UP) {
            return "text-bg-primary";
        }
        if (status == MarketingActionReportItem.Status.WITHOUT_RESULT) {
            return "text-bg-warning";
        }
        if (status == MarketingActionReportItem.Status.ARCHIVED) {
            return "text-bg-secondary";
        }
        return "text-bg-info";
    }

    private static String money(BigDecimal value) {
        BigDecimal safeValue = value != null ? value : BigDecimal.ZERO;
        return "S/ " + safeValue.setScale(2, RoundingMode.HALF_UP).toPlainString();
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

    public String getActionDateText() {
        return actionDateText;
    }

    public String getActionTypeLabel() {
        return actionTypeLabel;
    }

    public String getChannelLabel() {
        return channelLabel;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getStatusBadgeClass() {
        return statusBadgeClass;
    }

    public String getDescriptionPreview() {
        return descriptionPreview;
    }

    public String getObservedResultPreview() {
        return observedResultPreview;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public String getPublicationTitle() {
        return publicationTitle;
    }

    public String getIdeaTitle() {
        return ideaTitle;
    }

    public String getProductName() {
        return productName;
    }

    public String getGeneratedInquiriesText() {
        return generatedInquiriesText;
    }

    public String getEstimatedSalesText() {
        return estimatedSalesText;
    }

    public String getResponsibleDisplay() {
        return responsibleDisplay;
    }

    public boolean isCanFollowUp() {
        return canFollowUp;
    }

    public boolean isCanMarkWithResult() {
        return canMarkWithResult;
    }

    public boolean isCanMarkWithoutResult() {
        return canMarkWithoutResult;
    }

    public boolean isCanArchive() {
        return canArchive;
    }
}
