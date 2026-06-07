package com.ecoamazonas.eco_agua.product.cost;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class SalesChannelProfitabilitySummary {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final List<SalesChannelProfitabilityRow> rows;
    private final int channelCount;
    private final int activeChannelCount;
    private final int totalOrderCount;
    private final int paidOrderCount;
    private final int creditOrderCount;
    private final BigDecimal totalQuantitySold;
    private final BigDecimal totalRevenue;
    private final BigDecimal totalCost;
    private final BigDecimal totalGrossProfit;
    private final BigDecimal averageMarginPercent;
    private final SalesChannelProfitabilityRow mostProfitableChannel;
    private final SalesChannelProfitabilityRow topRevenueChannel;
    private final SalesChannelProfitabilityRow topSellingLowMarginChannel;

    public SalesChannelProfitabilitySummary(List<SalesChannelProfitabilityRow> rows) {
        this.rows = rows != null ? rows : List.of();
        this.channelCount = this.rows.size();
        this.activeChannelCount = (int) this.rows.stream().filter(SalesChannelProfitabilityRow::hasMovement).count();
        this.totalOrderCount = this.rows.stream().mapToInt(SalesChannelProfitabilityRow::getOrderCount).sum();
        this.paidOrderCount = this.rows.stream().mapToInt(SalesChannelProfitabilityRow::getPaidOrderCount).sum();
        this.creditOrderCount = this.rows.stream().mapToInt(SalesChannelProfitabilityRow::getCreditOrderCount).sum();
        this.totalQuantitySold = sumAmounts(this.rows.stream().map(SalesChannelProfitabilityRow::getQuantitySold).toList());
        this.totalRevenue = sumAmounts(this.rows.stream().map(SalesChannelProfitabilityRow::getRevenue).toList());
        this.totalCost = sumAmounts(this.rows.stream().map(SalesChannelProfitabilityRow::getTotalCost).toList());
        this.totalGrossProfit = sumAmounts(this.rows.stream().map(SalesChannelProfitabilityRow::getGrossProfit).toList());
        this.averageMarginPercent = calculateAverageMarginPercent();
        this.mostProfitableChannel = findMostProfitableChannel();
        this.topRevenueChannel = findTopRevenueChannel();
        this.topSellingLowMarginChannel = findTopSellingLowMarginChannel();
    }

    public List<SalesChannelProfitabilityRow> getRows() {
        return rows;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public int getActiveChannelCount() {
        return activeChannelCount;
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

    public BigDecimal getTotalQuantitySold() {
        return totalQuantitySold;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public BigDecimal getTotalGrossProfit() {
        return totalGrossProfit;
    }

    public BigDecimal getAverageMarginPercent() {
        return averageMarginPercent;
    }

    public SalesChannelProfitabilityRow getMostProfitableChannel() {
        return mostProfitableChannel;
    }

    public SalesChannelProfitabilityRow getTopRevenueChannel() {
        return topRevenueChannel;
    }

    public SalesChannelProfitabilityRow getTopSellingLowMarginChannel() {
        return topSellingLowMarginChannel;
    }

    public boolean hasMovement() {
        return totalOrderCount > 0 || totalRevenue.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal calculateAverageMarginPercent() {
        if (totalRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO;
        }
        return totalGrossProfit
                .multiply(ONE_HUNDRED)
                .divide(totalRevenue, 2, RoundingMode.HALF_UP);
    }

    private SalesChannelProfitabilityRow findMostProfitableChannel() {
        return rows.stream()
                .filter(SalesChannelProfitabilityRow::hasMovement)
                .max((left, right) -> left.getGrossProfit().compareTo(right.getGrossProfit()))
                .orElse(null);
    }

    private SalesChannelProfitabilityRow findTopRevenueChannel() {
        return rows.stream()
                .filter(SalesChannelProfitabilityRow::hasMovement)
                .max((left, right) -> left.getRevenue().compareTo(right.getRevenue()))
                .orElse(null);
    }

    private SalesChannelProfitabilityRow findTopSellingLowMarginChannel() {
        return rows.stream()
                .filter(SalesChannelProfitabilityRow::hasMovement)
                .filter(SalesChannelProfitabilityRow::isLowMargin)
                .max((left, right) -> left.getQuantitySold().compareTo(right.getQuantitySold()))
                .orElse(null);
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
