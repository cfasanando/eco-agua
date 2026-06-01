package com.ecoamazonas.eco_agua.order;

import java.math.BigDecimal;

public class ReceivableSummary {

    private final int openOrderCount;
    private final int clientCount;
    private final int overdueOrderCount;
    private final int dueTodayOrderCount;
    private final BigDecimal totalPendingAmount;
    private final BigDecimal overduePendingAmount;
    private final BigDecimal dueTodayPendingAmount;
    private final BigDecimal currentPendingAmount;

    public ReceivableSummary(
            int openOrderCount,
            int clientCount,
            int overdueOrderCount,
            int dueTodayOrderCount,
            BigDecimal totalPendingAmount,
            BigDecimal overduePendingAmount,
            BigDecimal dueTodayPendingAmount,
            BigDecimal currentPendingAmount
    ) {
        this.openOrderCount = openOrderCount;
        this.clientCount = clientCount;
        this.overdueOrderCount = overdueOrderCount;
        this.dueTodayOrderCount = dueTodayOrderCount;
        this.totalPendingAmount = totalPendingAmount;
        this.overduePendingAmount = overduePendingAmount;
        this.dueTodayPendingAmount = dueTodayPendingAmount;
        this.currentPendingAmount = currentPendingAmount;
    }

    public int getOpenOrderCount() {
        return openOrderCount;
    }

    public int getClientCount() {
        return clientCount;
    }

    public int getOverdueOrderCount() {
        return overdueOrderCount;
    }

    public int getDueTodayOrderCount() {
        return dueTodayOrderCount;
    }

    public BigDecimal getTotalPendingAmount() {
        return totalPendingAmount;
    }

    public BigDecimal getOverduePendingAmount() {
        return overduePendingAmount;
    }

    public BigDecimal getDueTodayPendingAmount() {
        return dueTodayPendingAmount;
    }

    public BigDecimal getCurrentPendingAmount() {
        return currentPendingAmount;
    }

    public boolean hasOverdueOrders() {
        return overdueOrderCount > 0;
    }
}
