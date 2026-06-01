package com.ecoamazonas.eco_agua.client;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class ClientPortfolioRow {

    private final Long clientId;
    private final String clientName;
    private final String profileName;
    private final String phone;
    private final String preferredSalesChannelLabel;
    private final LocalDate registrationDate;
    private final boolean historyAvailable;
    private final LocalDate lastOrderDate;
    private final long daysSinceLastOrder;
    private final int averageIntervalDays;
    private final long overdueDays;
    private final int commercialOrdersInPeriod;
    private final BigDecimal totalRevenueInPeriod;
    private final BigDecimal estimatedProfitInPeriod;
    private final BigDecimal averageTicketInPeriod;
    private final BigDecimal profitMarginPercentInPeriod;
    private final BigDecimal creditPendingAllTime;
    private final int borrowedBottlesInPeriod;
    private final int borrowedBottlesAllTime;
    private final int healthScore;
    private final String healthLabel;
    private final String healthBadgeClass;
    private final String recommendedAction;
    private final String strategyNote;
    private final boolean reactivationCandidate;
    private final boolean dormant;

    public ClientPortfolioRow(
            Long clientId,
            String clientName,
            String profileName,
            String phone,
            String preferredSalesChannelLabel,
            LocalDate registrationDate,
            boolean historyAvailable,
            LocalDate lastOrderDate,
            long daysSinceLastOrder,
            int averageIntervalDays,
            long overdueDays,
            int commercialOrdersInPeriod,
            BigDecimal totalRevenueInPeriod,
            BigDecimal estimatedProfitInPeriod,
            BigDecimal averageTicketInPeriod,
            BigDecimal profitMarginPercentInPeriod,
            BigDecimal creditPendingAllTime,
            int borrowedBottlesInPeriod,
            int borrowedBottlesAllTime,
            int healthScore,
            String healthLabel,
            String healthBadgeClass,
            String recommendedAction,
            String strategyNote,
            boolean reactivationCandidate,
            boolean dormant
    ) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.profileName = profileName;
        this.phone = phone;
        this.preferredSalesChannelLabel = preferredSalesChannelLabel;
        this.registrationDate = registrationDate;
        this.historyAvailable = historyAvailable;
        this.lastOrderDate = lastOrderDate;
        this.daysSinceLastOrder = daysSinceLastOrder;
        this.averageIntervalDays = averageIntervalDays;
        this.overdueDays = overdueDays;
        this.commercialOrdersInPeriod = commercialOrdersInPeriod;
        this.totalRevenueInPeriod = totalRevenueInPeriod;
        this.estimatedProfitInPeriod = estimatedProfitInPeriod;
        this.averageTicketInPeriod = averageTicketInPeriod;
        this.profitMarginPercentInPeriod = profitMarginPercentInPeriod;
        this.creditPendingAllTime = creditPendingAllTime;
        this.borrowedBottlesInPeriod = borrowedBottlesInPeriod;
        this.borrowedBottlesAllTime = borrowedBottlesAllTime;
        this.healthScore = healthScore;
        this.healthLabel = healthLabel;
        this.healthBadgeClass = healthBadgeClass;
        this.recommendedAction = recommendedAction;
        this.strategyNote = strategyNote;
        this.reactivationCandidate = reactivationCandidate;
        this.dormant = dormant;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getPhone() {
        return phone;
    }

    public String getPreferredSalesChannelLabel() {
        return preferredSalesChannelLabel;
    }

    public boolean isWhatsappContactAvailable() {
        return normalizeWhatsappNumber() != null;
    }

    public String getWhatsappContactUrl() {
        return buildWhatsappUrl("Hola, queremos coordinar contigo sobre tu pedido.");
    }

    public String getFollowUpWhatsappUrl() {
        return buildWhatsappUrl(getSuggestedWhatsappMessage());
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public boolean isHistoryAvailable() {
        return historyAvailable;
    }

    public LocalDate getLastOrderDate() {
        return lastOrderDate;
    }

    public long getDaysSinceLastOrder() {
        return daysSinceLastOrder;
    }

    public int getAverageIntervalDays() {
        return averageIntervalDays;
    }

    public long getOverdueDays() {
        return overdueDays;
    }

    public int getCommercialOrdersInPeriod() {
        return commercialOrdersInPeriod;
    }

    public BigDecimal getTotalRevenueInPeriod() {
        return totalRevenueInPeriod;
    }

    public BigDecimal getEstimatedProfitInPeriod() {
        return estimatedProfitInPeriod;
    }

    public BigDecimal getAverageTicketInPeriod() {
        return averageTicketInPeriod;
    }

    public BigDecimal getProfitMarginPercentInPeriod() {
        return profitMarginPercentInPeriod;
    }

    public BigDecimal getCreditPendingAllTime() {
        return creditPendingAllTime;
    }

    public int getBorrowedBottlesInPeriod() {
        return borrowedBottlesInPeriod;
    }

    public int getBorrowedBottlesAllTime() {
        return borrowedBottlesAllTime;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public String getHealthLabel() {
        return healthLabel;
    }

    public String getHealthBadgeClass() {
        return healthBadgeClass;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public String getStrategyNote() {
        return strategyNote;
    }

    public boolean isReactivationCandidate() {
        return reactivationCandidate;
    }

    public boolean isDormant() {
        return dormant;
    }

    public boolean isFollowUpRecommended() {
        return getFollowUpPriorityScore() >= 40;
    }

    public int getFollowUpPriorityScore() {
        int score = 0;

        if (creditPendingAllTime != null && creditPendingAllTime.compareTo(BigDecimal.ZERO) > 0) {
            score += 45;
        }

        if (dormant) {
            score += 35;
        } else if (reactivationCandidate) {
            score += 28;
        } else if (overdueDays > 0) {
            score += 20;
        }

        if (historyAvailable && daysSinceLastOrder >= 14) {
            score += 10;
        }

        if (healthScore >= 60 && commercialOrdersInPeriod > 0) {
            score += 8;
        }

        return Math.min(score, 100);
    }

    public String getFollowUpPriorityLabel() {
        if (creditPendingAllTime != null && creditPendingAllTime.compareTo(BigDecimal.ZERO) > 0) {
            return "Cobranza";
        }
        if (dormant) {
            return "Reactivar";
        }
        if (reactivationCandidate || overdueDays > 0) {
            return "Contactar";
        }
        if (healthScore >= 60 && commercialOrdersInPeriod > 0) {
            return "Fidelizar";
        }
        if (!historyAvailable) {
            return "Primer pedido";
        }
        return "Observar";
    }

    public String getFollowUpBadgeClass() {
        if (creditPendingAllTime != null && creditPendingAllTime.compareTo(BigDecimal.ZERO) > 0) {
            return "text-bg-danger";
        }
        if (dormant) {
            return "text-bg-warning";
        }
        if (reactivationCandidate || overdueDays > 0) {
            return "text-bg-primary";
        }
        if (healthScore >= 60 && commercialOrdersInPeriod > 0) {
            return "text-bg-success";
        }
        return "text-bg-secondary";
    }

    public String getFollowUpReason() {
        if (creditPendingAllTime != null && creditPendingAllTime.compareTo(BigDecimal.ZERO) > 0) {
            return "Tiene fiado pendiente. Primero conviene coordinar cobranza antes de ofrecer más crédito.";
        }
        if (dormant) {
            return "Lleva varios días sin comprar. Conviene reactivarlo con un mensaje directo o una promoción puntual.";
        }
        if (reactivationCandidate || overdueDays > 0) {
            return "Ya salió de su ciclo habitual de compra. Conviene contactarlo para evitar que se enfríe.";
        }
        if (healthScore >= 60 && commercialOrdersInPeriod > 0) {
            return "Cliente con buen movimiento. Conviene fidelizarlo, pedir referidos o recordarle productos disponibles.";
        }
        if (!historyAvailable) {
            return "Cliente activo sin historial de pedidos. Conviene registrar una primera compra para empezar seguimiento.";
        }
        return "Mantener en observación y revisar su comportamiento en los siguientes días.";
    }

    public String getSuggestedWhatsappMessage() {
        if (creditPendingAllTime != null && creditPendingAllTime.compareTo(BigDecimal.ZERO) > 0) {
            return "Hola, te escribimos para coordinar tu saldo pendiente y revisar si necesitas algún producto disponible.";
        }
        if (dormant || reactivationCandidate || overdueDays > 0) {
            return "Hola, te escribimos para contarte qué productos tenemos disponibles esta semana. ¿Deseas que te enviemos la lista?";
        }
        if (healthScore >= 60 && commercialOrdersInPeriod > 0) {
            return "Hola, gracias por comprar con nosotros. Tenemos productos disponibles esta semana, ¿te compartimos la lista?";
        }
        return "Hola, te escribimos para coordinar contigo y comentarte los productos disponibles esta semana.";
    }

    private String buildWhatsappUrl(String message) {
        String normalizedPhone = normalizeWhatsappNumber();
        if (normalizedPhone == null) {
            return null;
        }

        String safeMessage = message != null && !message.isBlank()
                ? message
                : "Hola, queremos coordinar contigo sobre tu pedido.";
        return "https://wa.me/" + normalizedPhone + "?text=" + URLEncoder.encode(safeMessage, StandardCharsets.UTF_8);
    }

    private String normalizeWhatsappNumber() {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        String digits = phone.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }

        if (digits.length() == 9 && digits.startsWith("9")) {
            return "51" + digits;
        }

        if (digits.length() >= 10) {
            return digits;
        }

        return null;
    }
}
