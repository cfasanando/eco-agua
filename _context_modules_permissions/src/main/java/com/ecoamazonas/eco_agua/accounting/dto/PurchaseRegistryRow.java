package com.ecoamazonas.eco_agua.accounting.dto;

import com.ecoamazonas.eco_agua.expense.ExpenseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PurchaseRegistryRow {

    private final LocalDate emissionDate;
    private final String docType;
    private final String docSeries;
    private final String docNumber;

    private final String supplierDocType;
    private final String supplierDocNumber;
    private final String supplierName;

    private final BigDecimal taxBase;
    private final BigDecimal taxIgv;
    private final BigDecimal total;

    // We keep it as String for Thymeleaf, but the constructor receives ExpenseStatus
    private final String status;

    private final String categoryName;
    private final boolean igvCreditUsable;
    private final String origin;

    public PurchaseRegistryRow(
            LocalDate emissionDate,
            String docType,
            String docSeries,
            String docNumber,
            String supplierDocType,
            String supplierDocNumber,
            String supplierName,
            BigDecimal taxBase,
            BigDecimal taxIgv,
            BigDecimal total,
            ExpenseStatus status,      // 👈 aquí ahora es ExpenseStatus
            String categoryName,
            boolean igvCreditUsable,
            String origin
    ) {
        this.emissionDate = emissionDate;
        this.docType = docType;
        this.docSeries = docSeries;
        this.docNumber = docNumber;
        this.supplierDocType = supplierDocType;
        this.supplierDocNumber = supplierDocNumber;
        this.supplierName = supplierName;
        this.taxBase = taxBase;
        this.taxIgv = taxIgv;
        this.total = total;
        this.status = status != null ? status.name() : null;  // lo convertimos a String
        this.categoryName = categoryName;
        this.igvCreditUsable = igvCreditUsable;
        this.origin = origin;
    }

    public LocalDate getEmissionDate() {
        return emissionDate;
    }

    public String getDocType() {
        return docType;
    }

    public String getDocSeries() {
        return docSeries;
    }

    public String getDocNumber() {
        return docNumber;
    }

    public String getSupplierDocType() {
        return supplierDocType;
    }

    public String getSupplierDocNumber() {
        return supplierDocNumber;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public BigDecimal getTaxBase() {
        return taxBase;
    }

    public BigDecimal getTaxIgv() {
        return taxIgv;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getStatus() {
        return status;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public boolean isIgvCreditUsable() {
        return igvCreditUsable;
    }

    public String getOrigin() {
        return origin;
    }
}
