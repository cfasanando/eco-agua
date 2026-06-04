package com.ecoamazonas.eco_agua.user;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class HrMonthlyPayrollSummary {

    private int activeEmployeeCount;
    private int paidEmployeeCount;
    private int paymentCount;
    private int activeObligationCount;
    private BigDecimal totalConfiguredAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal totalGross = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal totalDiscount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal totalNet = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal totalPendingObligations = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public int getActiveEmployeeCount() {
        return activeEmployeeCount;
    }

    public void setActiveEmployeeCount(int activeEmployeeCount) {
        this.activeEmployeeCount = activeEmployeeCount;
    }

    public int getPaidEmployeeCount() {
        return paidEmployeeCount;
    }

    public void setPaidEmployeeCount(int paidEmployeeCount) {
        this.paidEmployeeCount = paidEmployeeCount;
    }

    public int getPaymentCount() {
        return paymentCount;
    }

    public void setPaymentCount(int paymentCount) {
        this.paymentCount = paymentCount;
    }

    public int getActiveObligationCount() {
        return activeObligationCount;
    }

    public void setActiveObligationCount(int activeObligationCount) {
        this.activeObligationCount = activeObligationCount;
    }

    public BigDecimal getTotalConfiguredAmount() {
        return totalConfiguredAmount;
    }

    public void setTotalConfiguredAmount(BigDecimal totalConfiguredAmount) {
        this.totalConfiguredAmount = normalizeMoney(totalConfiguredAmount);
    }

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

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
