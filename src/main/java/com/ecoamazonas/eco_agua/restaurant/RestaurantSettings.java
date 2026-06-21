package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public record RestaurantSettings(
        String tradeName,
        String legalName,
        String ruc,
        String address,
        String phone,
        String whatsapp,
        String logoPath,
        String currencySymbol,
        boolean igvEnabled,
        BigDecimal igvRate,
        boolean serviceChargeEnabled,
        BigDecimal serviceChargeRate,
        String orderPrefix,
        int preparationMinutes,
        String openingTime,
        String closingTime,
        String publicWelcomeMessage,
        String receiptFooter,
        boolean qrOrdersEnabled,
        boolean tableRequestsEnabled,
        boolean takeawayEnabled,
        boolean deliveryEnabled,
        BigDecimal defaultDeliveryFee,
        int qrMaxItems,
        int qrMaxQuantityPerItem,
        boolean ticketShowLogo
) {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public String safeTradeName() {
        return blankToFallback(tradeName, "Restaurante");
    }

    public String safeLegalName() {
        return blankToFallback(legalName, safeTradeName());
    }

    public String safeCurrencySymbol() {
        return blankToFallback(currencySymbol, "S/");
    }

    public String safeOrderPrefix() {
        return blankToFallback(orderPrefix, "CMD");
    }

    public BigDecimal safeIgvRate() {
        return igvRate == null ? BigDecimal.ZERO : igvRate;
    }

    public BigDecimal safeServiceChargeRate() {
        return serviceChargeRate == null ? BigDecimal.ZERO : serviceChargeRate;
    }

    public BigDecimal safeDefaultDeliveryFee() {
        return defaultDeliveryFee == null ? BigDecimal.ZERO : defaultDeliveryFee;
    }

    public String openingHoursLabel() {
        if (openingTime == null || openingTime.isBlank() || closingTime == null || closingTime.isBlank()) {
            return "Horario no configurado";
        }
        return openingTime + " - " + closingTime;
    }

    public boolean isOpenNow() {
        try {
            LocalTime now = LocalTime.now();
            LocalTime open = LocalTime.parse(openingTime, TIME_FORMAT);
            LocalTime close = LocalTime.parse(closingTime, TIME_FORMAT);
            if (open.equals(close)) {
                return true;
            }
            if (open.isBefore(close)) {
                return !now.isBefore(open) && now.isBefore(close);
            }
            return !now.isBefore(open) || now.isBefore(close);
        } catch (RuntimeException ex) {
            return true;
        }
    }

    public boolean showIgvBreakdown() {
        return igvEnabled && safeIgvRate().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean applyServiceChargeTo(String serviceType) {
        return serviceChargeEnabled
                && safeServiceChargeRate().compareTo(BigDecimal.ZERO) > 0
                && "DINE_IN".equalsIgnoreCase(serviceType);
    }

    private String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
