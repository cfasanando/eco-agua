package com.ecoamazonas.eco_agua.marketing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class MarketingFeaturedProductRow {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Long id;
    private final String title;
    private final String productName;
    private final String productPriceText;
    private final String imagePath;
    private final String shortTextPreview;
    private final String statusLabel;
    private final String statusBadgeClass;
    private final String displayPlaceLabel;
    private final String priorityText;
    private final String dateRangeText;
    private final String callToActionDisplay;
    private final boolean productMarkedFeatured;
    private final boolean canActivate;
    private final boolean canPause;
    private final boolean canFinish;
    private final boolean canArchive;

    private MarketingFeaturedProductRow(Long id,
                                        String title,
                                        String productName,
                                        String productPriceText,
                                        String imagePath,
                                        String shortTextPreview,
                                        String statusLabel,
                                        String statusBadgeClass,
                                        String displayPlaceLabel,
                                        String priorityText,
                                        String dateRangeText,
                                        String callToActionDisplay,
                                        boolean productMarkedFeatured,
                                        boolean canActivate,
                                        boolean canPause,
                                        boolean canFinish,
                                        boolean canArchive) {
        this.id = id;
        this.title = title;
        this.productName = productName;
        this.productPriceText = productPriceText;
        this.imagePath = imagePath;
        this.shortTextPreview = shortTextPreview;
        this.statusLabel = statusLabel;
        this.statusBadgeClass = statusBadgeClass;
        this.displayPlaceLabel = displayPlaceLabel;
        this.priorityText = priorityText;
        this.dateRangeText = dateRangeText;
        this.callToActionDisplay = callToActionDisplay;
        this.productMarkedFeatured = productMarkedFeatured;
        this.canActivate = canActivate;
        this.canPause = canPause;
        this.canFinish = canFinish;
        this.canArchive = canArchive;
    }

    public static MarketingFeaturedProductRow from(MarketingFeaturedProduct item) {
        MarketingFeaturedProduct.Status status = item.getStatus();
        return new MarketingFeaturedProductRow(
                item.getId(),
                item.getTitleDisplay(),
                safe(() -> item.getProduct() != null ? item.getProduct().getName() : null),
                money(safeMoney(() -> item.getProduct() != null ? item.getProduct().getPrice() : null)),
                safe(() -> item.getProduct() != null ? item.getProduct().getImagePath() : null),
                item.getShortTextPreview(),
                item.getStatusLabel(),
                badgeClass(status),
                item.getDisplayPlaceLabel(),
                String.valueOf(item.getPriority() != null ? item.getPriority() : 1),
                dateRange(item),
                item.getCallToActionDisplay(),
                Boolean.TRUE.equals(safeBoolean(() -> item.getProduct() != null && item.getProduct().isFeatured())),
                status != MarketingFeaturedProduct.Status.ACTIVE && status != MarketingFeaturedProduct.Status.ARCHIVED,
                status == MarketingFeaturedProduct.Status.ACTIVE,
                status != MarketingFeaturedProduct.Status.FINISHED && status != MarketingFeaturedProduct.Status.ARCHIVED,
                status != MarketingFeaturedProduct.Status.ARCHIVED
        );
    }

    private static String badgeClass(MarketingFeaturedProduct.Status status) {
        if (status == MarketingFeaturedProduct.Status.ACTIVE) {
            return "text-bg-success";
        }
        if (status == MarketingFeaturedProduct.Status.PAUSED) {
            return "text-bg-warning";
        }
        if (status == MarketingFeaturedProduct.Status.FINISHED) {
            return "text-bg-primary";
        }
        if (status == MarketingFeaturedProduct.Status.ARCHIVED) {
            return "text-bg-secondary";
        }
        return "text-bg-info";
    }

    private static String dateRange(MarketingFeaturedProduct item) {
        if (item.getStartDate() == null && item.getEndDate() == null) {
            return "Sin fechas definidas";
        }
        if (item.getStartDate() != null && item.getEndDate() != null) {
            return item.getStartDate().format(DATE_FORMATTER) + " - " + item.getEndDate().format(DATE_FORMATTER);
        }
        if (item.getStartDate() != null) {
            return "Desde " + item.getStartDate().format(DATE_FORMATTER);
        }
        return "Hasta " + item.getEndDate().format(DATE_FORMATTER);
    }

    private static String money(BigDecimal value) {
        BigDecimal safeValue = value != null ? value : BigDecimal.ZERO;
        return "S/ " + safeValue.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String safe(Supplier<String> supplier) {
        try {
            return trimToNull(supplier.get());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static BigDecimal safeMoney(Supplier<BigDecimal> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static Boolean safeBoolean(Supplier<Boolean> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException exception) {
            return Boolean.FALSE;
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

    public String getProductName() {
        return productName;
    }

    public String getProductPriceText() {
        return productPriceText;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getShortTextPreview() {
        return shortTextPreview;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getStatusBadgeClass() {
        return statusBadgeClass;
    }

    public String getDisplayPlaceLabel() {
        return displayPlaceLabel;
    }

    public String getPriorityText() {
        return priorityText;
    }

    public String getDateRangeText() {
        return dateRangeText;
    }

    public String getCallToActionDisplay() {
        return callToActionDisplay;
    }

    public boolean isProductMarkedFeatured() {
        return productMarkedFeatured;
    }

    public boolean isCanActivate() {
        return canActivate;
    }

    public boolean isCanPause() {
        return canPause;
    }

    public boolean isCanFinish() {
        return canFinish;
    }

    public boolean isCanArchive() {
        return canArchive;
    }
}
