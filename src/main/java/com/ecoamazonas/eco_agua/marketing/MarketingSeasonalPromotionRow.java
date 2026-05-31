package com.ecoamazonas.eco_agua.marketing;

import com.ecoamazonas.eco_agua.promotion.Promotion;
import com.ecoamazonas.eco_agua.promotion.PromotionProduct;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MarketingSeasonalPromotionRow {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Long id;
    private final String name;
    private final String descriptionPreview;
    private final String dateRangeText;
    private final String statusLabel;
    private final String statusBadgeClass;
    private final String colorBorder;
    private final String campaignCodeText;
    private final String maxCounterText;
    private final String productSummary;
    private final String productConfig;
    private final boolean hasBanner;
    private final boolean canActivate;
    private final boolean canPause;
    private final boolean canFinish;
    private final boolean endingSoon;

    private MarketingSeasonalPromotionRow(Long id,
                                          String name,
                                          String descriptionPreview,
                                          String dateRangeText,
                                          String statusLabel,
                                          String statusBadgeClass,
                                          String colorBorder,
                                          String campaignCodeText,
                                          String maxCounterText,
                                          String productSummary,
                                          String productConfig,
                                          boolean hasBanner,
                                          boolean canActivate,
                                          boolean canPause,
                                          boolean canFinish,
                                          boolean endingSoon) {
        this.id = id;
        this.name = name;
        this.descriptionPreview = descriptionPreview;
        this.dateRangeText = dateRangeText;
        this.statusLabel = statusLabel;
        this.statusBadgeClass = statusBadgeClass;
        this.colorBorder = colorBorder;
        this.campaignCodeText = campaignCodeText;
        this.maxCounterText = maxCounterText;
        this.productSummary = productSummary;
        this.productConfig = productConfig;
        this.hasBanner = hasBanner;
        this.canActivate = canActivate;
        this.canPause = canPause;
        this.canFinish = canFinish;
        this.endingSoon = endingSoon;
    }

    public static MarketingSeasonalPromotionRow from(Promotion promotion, LocalDate today) {
        PromotionStatus status = resolveStatus(promotion, today);
        boolean active = status == PromotionStatus.ACTIVE;
        boolean paused = status == PromotionStatus.PAUSED;
        boolean finished = status == PromotionStatus.FINISHED;
        boolean endingSoon = active && promotion.getEndDate() != null
                && !promotion.getEndDate().isBefore(today)
                && !promotion.getEndDate().isAfter(today.plusDays(7));

        return new MarketingSeasonalPromotionRow(
                promotion.getId(),
                textOrDefault(promotion.getName(), "Promoción sin nombre"),
                preview(promotion.getDescription(), 140),
                dateRange(promotion),
                status.label,
                status.badgeClass,
                textOrDefault(promotion.getColorBorder(), "#166534"),
                promotion.getPromoNumber() != null ? String.valueOf(promotion.getPromoNumber()) : "Sin código",
                promotion.getMaxCounter() != null ? promotion.getMaxCounter() + " usos" : "Sin límite",
                productSummary(promotion),
                productConfig(promotion),
                !isBlank(promotion.getBannerImagePath()),
                !active,
                active,
                !finished,
                endingSoon
        );
    }

    private static PromotionStatus resolveStatus(Promotion promotion, LocalDate today) {
        if (!promotion.isEnabled()) {
            return PromotionStatus.PAUSED;
        }
        if (promotion.getEndDate() != null && promotion.getEndDate().isBefore(today)) {
            return PromotionStatus.FINISHED;
        }
        if (promotion.getStartDate() != null && promotion.getStartDate().isAfter(today)) {
            return PromotionStatus.PLANNED;
        }
        return PromotionStatus.ACTIVE;
    }

    private static String dateRange(Promotion promotion) {
        if (promotion.getStartDate() == null && promotion.getEndDate() == null) {
            return "Sin fechas definidas";
        }
        if (promotion.getStartDate() != null && promotion.getEndDate() != null) {
            return promotion.getStartDate().format(DATE_FORMATTER) + " - " + promotion.getEndDate().format(DATE_FORMATTER);
        }
        if (promotion.getStartDate() != null) {
            return "Desde " + promotion.getStartDate().format(DATE_FORMATTER);
        }
        return "Hasta " + promotion.getEndDate().format(DATE_FORMATTER);
    }

    private static String productSummary(Promotion promotion) {
        if (promotion.getPromotionProducts() == null || promotion.getPromotionProducts().isEmpty()) {
            return "Sin productos configurados";
        }

        List<String> names = new ArrayList<>();
        for (PromotionProduct item : promotion.getPromotionProducts()) {
            if (item != null && item.getProduct() != null && !isBlank(item.getProduct().getName())) {
                names.add(item.getProduct().getName());
            }
        }

        if (names.isEmpty()) {
            return "Sin productos configurados";
        }
        if (names.size() <= 2) {
            return String.join(", ", names);
        }
        return names.get(0) + ", " + names.get(1) + " y " + (names.size() - 2) + " más";
    }

    private static String productConfig(Promotion promotion) {
        if (promotion.getPromotionProducts() == null || promotion.getPromotionProducts().isEmpty()) {
            return "";
        }

        List<String> values = new ArrayList<>();
        for (PromotionProduct item : promotion.getPromotionProducts()) {
            if (item == null || item.getProduct() == null || item.getProduct().getId() == null) {
                continue;
            }
            String quantity = item.getQuantity() != null ? String.valueOf(item.getQuantity()) : "0";
            String amount = moneyValue(item.getAmount());
            values.add(item.getProduct().getId() + ":" + quantity + ":" + amount);
        }
        return String.join(";", values);
    }

    private static String moneyValue(BigDecimal amount) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        return safeAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String preview(String value, int maxLength) {
        if (isBlank(value)) {
            return "Sin mensaje promocional registrado.";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static String textOrDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private enum PromotionStatus {
        PLANNED("Planificada", "text-bg-info"),
        ACTIVE("Activa", "text-bg-success"),
        PAUSED("Pausada", "text-bg-warning"),
        FINISHED("Finalizada", "text-bg-secondary");

        private final String label;
        private final String badgeClass;

        PromotionStatus(String label, String badgeClass) {
            this.label = label;
            this.badgeClass = badgeClass;
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescriptionPreview() {
        return descriptionPreview;
    }

    public String getDateRangeText() {
        return dateRangeText;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getStatusBadgeClass() {
        return statusBadgeClass;
    }

    public String getColorBorder() {
        return colorBorder;
    }

    public String getCampaignCodeText() {
        return campaignCodeText;
    }

    public String getMaxCounterText() {
        return maxCounterText;
    }

    public String getProductSummary() {
        return productSummary;
    }

    public String getProductConfig() {
        return productConfig;
    }

    public boolean isHasBanner() {
        return hasBanner;
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

    public boolean isEndingSoon() {
        return endingSoon;
    }
}
