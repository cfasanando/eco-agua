package com.ecoamazonas.eco_agua.expense;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AccountsPayableSupplierSummary {

    private Long supplierId;
    private String supplierName;
    private String phone;
    private int debtCount;
    private int overdueDebtCount;
    private BigDecimal pendingAmount = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private LocalDate nearestDueDate;
    private String priorityLabel;
    private String priorityBadgeClass;
    private String whatsappUrl;

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getDebtCount() {
        return debtCount;
    }

    public void setDebtCount(int debtCount) {
        this.debtCount = debtCount;
    }

    public int getOverdueDebtCount() {
        return overdueDebtCount;
    }

    public void setOverdueDebtCount(int overdueDebtCount) {
        this.overdueDebtCount = overdueDebtCount;
    }

    public BigDecimal getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(BigDecimal pendingAmount) {
        this.pendingAmount = pendingAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public LocalDate getNearestDueDate() {
        return nearestDueDate;
    }

    public void setNearestDueDate(LocalDate nearestDueDate) {
        this.nearestDueDate = nearestDueDate;
    }

    public String getPriorityLabel() {
        return priorityLabel;
    }

    public void setPriorityLabel(String priorityLabel) {
        this.priorityLabel = priorityLabel;
    }

    public String getPriorityBadgeClass() {
        return priorityBadgeClass;
    }

    public void setPriorityBadgeClass(String priorityBadgeClass) {
        this.priorityBadgeClass = priorityBadgeClass;
    }

    public String getWhatsappUrl() {
        return whatsappUrl;
    }

    public void setWhatsappUrl(String whatsappUrl) {
        this.whatsappUrl = whatsappUrl;
    }

    public boolean isWhatsappContactAvailable() {
        return whatsappUrl != null && !whatsappUrl.isBlank();
    }
}
