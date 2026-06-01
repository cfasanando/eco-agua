package com.ecoamazonas.eco_agua.order;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReceivableClientSummary {

    private final Long clientId;
    private final String clientName;
    private final String phone;
    private final int orderCount;
    private final int overdueOrderCount;
    private final BigDecimal pendingAmount;
    private final LocalDate oldestOrderDate;
    private final LocalDate nearestDueDate;
    private final String mainSalesChannelLabel;
    private final String whatsappUrl;

    public ReceivableClientSummary(
            Long clientId,
            String clientName,
            String phone,
            int orderCount,
            int overdueOrderCount,
            BigDecimal pendingAmount,
            LocalDate oldestOrderDate,
            LocalDate nearestDueDate,
            String mainSalesChannelLabel,
            String whatsappUrl
    ) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.phone = phone;
        this.orderCount = orderCount;
        this.overdueOrderCount = overdueOrderCount;
        this.pendingAmount = pendingAmount;
        this.oldestOrderDate = oldestOrderDate;
        this.nearestDueDate = nearestDueDate;
        this.mainSalesChannelLabel = mainSalesChannelLabel;
        this.whatsappUrl = whatsappUrl;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public String getPhone() {
        return phone;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public int getOverdueOrderCount() {
        return overdueOrderCount;
    }

    public BigDecimal getPendingAmount() {
        return pendingAmount;
    }

    public LocalDate getOldestOrderDate() {
        return oldestOrderDate;
    }

    public LocalDate getNearestDueDate() {
        return nearestDueDate;
    }

    public String getMainSalesChannelLabel() {
        return mainSalesChannelLabel;
    }

    public String getWhatsappUrl() {
        return whatsappUrl;
    }

    public boolean isWhatsappContactAvailable() {
        return whatsappUrl != null && !whatsappUrl.isBlank();
    }

    public String getPriorityLabel() {
        if (overdueOrderCount > 0) {
            return "Vencido";
        }

        if (nearestDueDate != null && nearestDueDate.isEqual(LocalDate.now())) {
            return "Vence hoy";
        }

        if (nearestDueDate == null) {
            return "Sin fecha";
        }

        return "Pendiente";
    }

    public String getPriorityBadgeClass() {
        if (overdueOrderCount > 0) {
            return "text-bg-danger";
        }

        if (nearestDueDate != null && nearestDueDate.isEqual(LocalDate.now())) {
            return "text-bg-warning";
        }

        if (nearestDueDate == null) {
            return "text-bg-secondary";
        }

        return "text-bg-primary";
    }
}
