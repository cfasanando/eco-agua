package com.ecoamazonas.eco_agua.product.cost;

import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.SalesChannel;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SalesChannelProfitabilityRow {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal LOW_MARGIN_THRESHOLD = BigDecimal.valueOf(20);

    private final SalesChannel channel;
    private int orderCount;
    private int paidOrderCount;
    private int creditOrderCount;
    private BigDecimal quantitySold = ZERO;
    private BigDecimal revenue = ZERO;
    private BigDecimal totalCost = ZERO;
    private BigDecimal grossProfit = ZERO;

    public SalesChannelProfitabilityRow(SalesChannel channel) {
        this.channel = channel;
    }

    public void addOrder(OrderStatus status) {
        orderCount++;
        if (status == OrderStatus.PAID) {
            paidOrderCount++;
        } else if (status == OrderStatus.CREDIT) {
            creditOrderCount++;
        }
    }

    public void addItem(BigDecimal quantity, BigDecimal itemRevenue, BigDecimal itemCost) {
        quantitySold = quantitySold.add(quantity(quantity)).setScale(2, RoundingMode.HALF_UP);
        revenue = revenue.add(money(itemRevenue)).setScale(2, RoundingMode.HALF_UP);
        totalCost = totalCost.add(money(itemCost)).setScale(2, RoundingMode.HALF_UP);
        grossProfit = revenue.subtract(totalCost).setScale(2, RoundingMode.HALF_UP);
    }

    public SalesChannel getChannel() {
        return channel;
    }

    public String getChannelCode() {
        return channel != null ? channel.name() : "UNKNOWN";
    }

    public String getChannelLabel() {
        return channel != null ? channel.getLabel() : "Sin canal";
    }

    public int getOrderCount() {
        return orderCount;
    }

    public int getPaidOrderCount() {
        return paidOrderCount;
    }

    public int getCreditOrderCount() {
        return creditOrderCount;
    }

    public BigDecimal getQuantitySold() {
        return quantitySold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit;
    }

    public BigDecimal getMarginPercent() {
        if (revenue.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return grossProfit
                .multiply(ONE_HUNDRED)
                .divide(revenue, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getAverageTicketAmount() {
        if (orderCount <= 0) {
            return ZERO;
        }
        return revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getAverageProfitPerOrder() {
        if (orderCount <= 0) {
            return ZERO;
        }
        return grossProfit.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
    }

    public boolean hasMovement() {
        return orderCount > 0 || quantitySold.compareTo(BigDecimal.ZERO) > 0 || revenue.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isLowMargin() {
        BigDecimal marginPercent = getMarginPercent();
        return marginPercent != null && marginPercent.compareTo(LOW_MARGIN_THRESHOLD) < 0;
    }

    public boolean isLoss() {
        return grossProfit.compareTo(BigDecimal.ZERO) < 0;
    }

    public String getReading() {
        if (!hasMovement()) {
            return "Sin movimiento";
        }
        if (isLoss()) {
            return "Pérdida estimada";
        }
        if (isLowMargin()) {
            return "Margen bajo";
        }
        return "Canal rentable";
    }

    public String getReadingBadgeClass() {
        if (!hasMovement()) {
            return "text-bg-light";
        }
        if (isLoss()) {
            return "text-bg-danger";
        }
        if (isLowMargin()) {
            return "text-bg-warning";
        }
        return "text-bg-success";
    }

    private BigDecimal money(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal quantity(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
