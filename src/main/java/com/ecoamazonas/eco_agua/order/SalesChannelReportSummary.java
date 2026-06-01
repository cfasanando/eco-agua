package com.ecoamazonas.eco_agua.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class SalesChannelReportSummary {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final List<SalesChannelReportRow> rows;
    private final int totalOrderCount;
    private final int paidOrderCount;
    private final int creditOrderCount;
    private final BigDecimal totalSalesAmount;
    private final BigDecimal paidAmount;
    private final BigDecimal pendingAmount;
    private final String bestChannelLabel;

    public SalesChannelReportSummary(List<SalesChannelReportRow> rows) {
        this.rows = rows != null ? rows : List.of();
        this.totalOrderCount = this.rows.stream().mapToInt(SalesChannelReportRow::getOrderCount).sum();
        this.paidOrderCount = this.rows.stream().mapToInt(SalesChannelReportRow::getPaidOrderCount).sum();
        this.creditOrderCount = this.rows.stream().mapToInt(SalesChannelReportRow::getCreditOrderCount).sum();
        this.totalSalesAmount = sumAmounts(this.rows.stream()
                .map(SalesChannelReportRow::getTotalSalesAmount)
                .toList());
        this.paidAmount = sumAmounts(this.rows.stream()
                .map(SalesChannelReportRow::getPaidAmount)
                .toList());
        this.pendingAmount = sumAmounts(this.rows.stream()
                .map(SalesChannelReportRow::getPendingAmount)
                .toList());
        this.bestChannelLabel = this.rows.stream()
                .filter(SalesChannelReportRow::hasMovement)
                .max((left, right) -> left.getTotalSalesAmount().compareTo(right.getTotalSalesAmount()))
                .map(SalesChannelReportRow::getChannelLabel)
                .orElse("Sin ventas");
    }

    public List<SalesChannelReportRow> getRows() {
        return rows;
    }

    public int getTotalOrderCount() {
        return totalOrderCount;
    }

    public int getPaidOrderCount() {
        return paidOrderCount;
    }

    public int getCreditOrderCount() {
        return creditOrderCount;
    }

    public BigDecimal getTotalSalesAmount() {
        return totalSalesAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public BigDecimal getPendingAmount() {
        return pendingAmount;
    }

    public BigDecimal getAverageTicketAmount() {
        if (totalOrderCount <= 0) {
            return ZERO;
        }

        return totalSalesAmount.divide(BigDecimal.valueOf(totalOrderCount), 2, RoundingMode.HALF_UP);
    }

    public String getBestChannelLabel() {
        return bestChannelLabel;
    }

    public boolean hasMovement() {
        return totalOrderCount > 0;
    }

    private BigDecimal sumAmounts(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return ZERO;
        }

        return values.stream()
                .filter(value -> value != null)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
