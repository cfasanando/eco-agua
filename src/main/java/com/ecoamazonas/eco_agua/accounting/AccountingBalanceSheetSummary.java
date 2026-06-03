package com.ecoamazonas.eco_agua.accounting;

import java.math.BigDecimal;

public class AccountingBalanceSheetSummary {

    private int totalAccounts;
    private int totalMovements;
    private BigDecimal totalAssets = BigDecimal.ZERO;
    private BigDecimal totalLiabilities = BigDecimal.ZERO;
    private BigDecimal totalEquity = BigDecimal.ZERO;
    private BigDecimal currentPeriodResult = BigDecimal.ZERO;

    public int getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(int totalAccounts) {
        this.totalAccounts = Math.max(totalAccounts, 0);
    }

    public int getTotalMovements() {
        return totalMovements;
    }

    public void setTotalMovements(int totalMovements) {
        this.totalMovements = Math.max(totalMovements, 0);
    }

    public BigDecimal getTotalAssets() {
        return totalAssets == null ? BigDecimal.ZERO : totalAssets;
    }

    public void setTotalAssets(BigDecimal totalAssets) {
        this.totalAssets = totalAssets == null ? BigDecimal.ZERO : totalAssets;
    }

    public BigDecimal getTotalLiabilities() {
        return totalLiabilities == null ? BigDecimal.ZERO : totalLiabilities;
    }

    public void setTotalLiabilities(BigDecimal totalLiabilities) {
        this.totalLiabilities = totalLiabilities == null ? BigDecimal.ZERO : totalLiabilities;
    }

    public BigDecimal getTotalEquity() {
        return totalEquity == null ? BigDecimal.ZERO : totalEquity;
    }

    public void setTotalEquity(BigDecimal totalEquity) {
        this.totalEquity = totalEquity == null ? BigDecimal.ZERO : totalEquity;
    }

    public BigDecimal getCurrentPeriodResult() {
        return currentPeriodResult == null ? BigDecimal.ZERO : currentPeriodResult;
    }

    public void setCurrentPeriodResult(BigDecimal currentPeriodResult) {
        this.currentPeriodResult = currentPeriodResult == null ? BigDecimal.ZERO : currentPeriodResult;
    }

    public BigDecimal getTotalLiabilitiesAndEquity() {
        return getTotalLiabilities().add(getTotalEquity());
    }

    public BigDecimal getDifference() {
        return getTotalAssets().subtract(getTotalLiabilitiesAndEquity());
    }

    public BigDecimal getAbsoluteDifference() {
        return getDifference().abs();
    }

    public boolean isBalanced() {
        return getAbsoluteDifference().compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isProfit() {
        return getCurrentPeriodResult().compareTo(BigDecimal.ZERO) >= 0;
    }

    public BigDecimal getAbsoluteCurrentPeriodResult() {
        return getCurrentPeriodResult().abs();
    }

    public String getCurrentPeriodResultLabel() {
        return isProfit() ? "Utilidad del período" : "Pérdida del período";
    }
}
