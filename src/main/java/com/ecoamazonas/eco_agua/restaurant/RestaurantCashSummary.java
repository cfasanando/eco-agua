package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RestaurantCashSummary(
        LocalDate businessDate,
        int paidOrders,
        BigDecimal paidTotal,
        BigDecimal cashTotal,
        BigDecimal cardTotal,
        BigDecimal yapeTotal,
        BigDecimal plinTotal,
        BigDecimal transferTotal,
        BigDecimal otherTotal,
        int openOrders,
        BigDecimal openTotal,
        int cancelledOrders,
        BigDecimal averageTicket
) {
    public BigDecimal safePaidTotal() {
        return paidTotal == null ? BigDecimal.ZERO : paidTotal;
    }

    public BigDecimal safeCashTotal() {
        return cashTotal == null ? BigDecimal.ZERO : cashTotal;
    }

    public BigDecimal safeCardTotal() {
        return cardTotal == null ? BigDecimal.ZERO : cardTotal;
    }

    public BigDecimal safeYapeTotal() {
        return yapeTotal == null ? BigDecimal.ZERO : yapeTotal;
    }

    public BigDecimal safePlinTotal() {
        return plinTotal == null ? BigDecimal.ZERO : plinTotal;
    }

    public BigDecimal safeTransferTotal() {
        return transferTotal == null ? BigDecimal.ZERO : transferTotal;
    }

    public BigDecimal safeOtherTotal() {
        return otherTotal == null ? BigDecimal.ZERO : otherTotal;
    }

    public BigDecimal safeOpenTotal() {
        return openTotal == null ? BigDecimal.ZERO : openTotal;
    }

    public BigDecimal safeAverageTicket() {
        return averageTicket == null ? BigDecimal.ZERO : averageTicket;
    }
}
