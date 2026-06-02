package com.ecoamazonas.eco_agua.expense;

import java.math.BigDecimal;

public class AccountsPayableSummary {

    private BigDecimal totalPendingAmount = BigDecimal.ZERO;
    private BigDecimal overduePendingAmount = BigDecimal.ZERO;
    private BigDecimal dueTodayPendingAmount = BigDecimal.ZERO;
    private BigDecimal partialPaidAmount = BigDecimal.ZERO;
    private int openDebtCount;
    private int supplierCount;
    private int overdueDebtCount;
    private int dueTodayDebtCount;
    private int partialDebtCount;

    public BigDecimal getTotalPendingAmount() {
        return totalPendingAmount;
    }

    public void setTotalPendingAmount(BigDecimal totalPendingAmount) {
        this.totalPendingAmount = totalPendingAmount;
    }

    public BigDecimal getOverduePendingAmount() {
        return overduePendingAmount;
    }

    public void setOverduePendingAmount(BigDecimal overduePendingAmount) {
        this.overduePendingAmount = overduePendingAmount;
    }

    public BigDecimal getDueTodayPendingAmount() {
        return dueTodayPendingAmount;
    }

    public void setDueTodayPendingAmount(BigDecimal dueTodayPendingAmount) {
        this.dueTodayPendingAmount = dueTodayPendingAmount;
    }

    public BigDecimal getPartialPaidAmount() {
        return partialPaidAmount;
    }

    public void setPartialPaidAmount(BigDecimal partialPaidAmount) {
        this.partialPaidAmount = partialPaidAmount;
    }

    public int getOpenDebtCount() {
        return openDebtCount;
    }

    public void setOpenDebtCount(int openDebtCount) {
        this.openDebtCount = openDebtCount;
    }

    public int getSupplierCount() {
        return supplierCount;
    }

    public void setSupplierCount(int supplierCount) {
        this.supplierCount = supplierCount;
    }

    public int getOverdueDebtCount() {
        return overdueDebtCount;
    }

    public void setOverdueDebtCount(int overdueDebtCount) {
        this.overdueDebtCount = overdueDebtCount;
    }

    public int getDueTodayDebtCount() {
        return dueTodayDebtCount;
    }

    public void setDueTodayDebtCount(int dueTodayDebtCount) {
        this.dueTodayDebtCount = dueTodayDebtCount;
    }

    public int getPartialDebtCount() {
        return partialDebtCount;
    }

    public void setPartialDebtCount(int partialDebtCount) {
        this.partialDebtCount = partialDebtCount;
    }

    public boolean hasOverdueDebts() {
        return overdueDebtCount > 0;
    }
}
