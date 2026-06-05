package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class ProductionReportProductRow {

    private final Long productId;
    private final String productName;
    private long orderCount;
    private BigDecimal quantityExpected = BigDecimal.ZERO;
    private BigDecimal quantityProduced = BigDecimal.ZERO;
    private BigDecimal quantityLoss = BigDecimal.ZERO;
    private BigDecimal inputCost = BigDecimal.ZERO;
    private LocalDate latestProductionDate;

    public ProductionReportProductRow(Long productId, String productName) {
        this.productId = productId;
        this.productName = productName;
    }

    public void addOrder(ProductionOrder order) {
        if (order == null) {
            return;
        }

        orderCount++;
        quantityExpected = quantityExpected.add(valueOrZero(order.getQuantityExpected()));
        quantityProduced = quantityProduced.add(valueOrZero(order.getQuantityProduced()));
        quantityLoss = quantityLoss.add(valueOrZero(order.getQuantityLoss()));
        inputCost = inputCost.add(valueOrZero(order.getTotalInputCost()));

        if (order.getProductionDate() != null
                && (latestProductionDate == null || order.getProductionDate().isAfter(latestProductionDate))) {
            latestProductionDate = order.getProductionDate();
        }
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public BigDecimal getQuantityExpected() {
        return quantityExpected;
    }

    public BigDecimal getQuantityProduced() {
        return quantityProduced;
    }

    public BigDecimal getQuantityLoss() {
        return quantityLoss;
    }

    public BigDecimal getInputCost() {
        return inputCost;
    }

    public BigDecimal getAverageRealUnitCost() {
        if (quantityProduced == null || quantityProduced.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        return inputCost.divide(quantityProduced, 4, RoundingMode.HALF_UP);
    }

    public BigDecimal getLossRatePercent() {
        if (quantityExpected == null || quantityExpected.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return quantityLoss.multiply(BigDecimal.valueOf(100)).divide(quantityExpected, 2, RoundingMode.HALF_UP);
    }

    public LocalDate getLatestProductionDate() {
        return latestProductionDate;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
