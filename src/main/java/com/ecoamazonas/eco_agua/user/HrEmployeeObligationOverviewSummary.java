package com.ecoamazonas.eco_agua.user;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class HrEmployeeObligationOverviewSummary {

    private int employeeCount;
    private int obligationCount;
    private int activeObligationCount;
    private int closedObligationCount;
    private BigDecimal totalOriginalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal totalAppliedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal totalPendingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public int getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(int employeeCount) {
        this.employeeCount = employeeCount;
    }

    public int getObligationCount() {
        return obligationCount;
    }

    public void setObligationCount(int obligationCount) {
        this.obligationCount = obligationCount;
    }

    public int getActiveObligationCount() {
        return activeObligationCount;
    }

    public void setActiveObligationCount(int activeObligationCount) {
        this.activeObligationCount = activeObligationCount;
    }

    public int getClosedObligationCount() {
        return closedObligationCount;
    }

    public void setClosedObligationCount(int closedObligationCount) {
        this.closedObligationCount = closedObligationCount;
    }

    public BigDecimal getTotalOriginalAmount() {
        return totalOriginalAmount;
    }

    public void setTotalOriginalAmount(BigDecimal totalOriginalAmount) {
        this.totalOriginalAmount = normalizeMoney(totalOriginalAmount);
    }

    public BigDecimal getTotalAppliedAmount() {
        return totalAppliedAmount;
    }

    public void setTotalAppliedAmount(BigDecimal totalAppliedAmount) {
        this.totalAppliedAmount = normalizeMoney(totalAppliedAmount);
    }

    public BigDecimal getTotalPendingAmount() {
        return totalPendingAmount;
    }

    public void setTotalPendingAmount(BigDecimal totalPendingAmount) {
        this.totalPendingAmount = normalizeMoney(totalPendingAmount);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
