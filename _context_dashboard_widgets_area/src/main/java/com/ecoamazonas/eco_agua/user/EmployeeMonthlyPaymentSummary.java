package com.ecoamazonas.eco_agua.user;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class EmployeeMonthlyPaymentSummary {

    private BigDecimal totalGross = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal totalDiscount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal totalNet = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal totalPendingObligations = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private int paymentCount;

    public BigDecimal getTotalGross() {
        return totalGross;
    }

    public void setTotalGross(BigDecimal totalGross) {
        this.totalGross = normalizeMoney(totalGross);
    }

    public BigDecimal getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(BigDecimal totalDiscount) {
        this.totalDiscount = normalizeMoney(totalDiscount);
    }

    public BigDecimal getTotalNet() {
        return totalNet;
    }

    public void setTotalNet(BigDecimal totalNet) {
        this.totalNet = normalizeMoney(totalNet);
    }

    public BigDecimal getTotalPendingObligations() {
        return totalPendingObligations;
    }

    public void setTotalPendingObligations(BigDecimal totalPendingObligations) {
        this.totalPendingObligations = normalizeMoney(totalPendingObligations);
    }

    public int getPaymentCount() {
        return paymentCount;
    }

    public void setPaymentCount(int paymentCount) {
        this.paymentCount = paymentCount;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
