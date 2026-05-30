package com.ecoamazonas.eco_agua.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SalesRegistryRow {

    private LocalDate emissionDate;
    private String docType;
    private String docSeries;
    private String docNumber;
    private String clientDocType;
    private String clientDocNumber;
    private String clientName;
    private BigDecimal taxBase;
    private BigDecimal taxIgv;
    private BigDecimal total;
    private String status;   // VÁLIDO, ANULADO, BORRADOR
    private String origin;   // SALE_ORDER / OTHER_INCOME
    private Long originId;

    // Constructors

    public SalesRegistryRow() {
    }

    public SalesRegistryRow(
            LocalDate emissionDate,
            String docType,
            String docSeries,
            String docNumber,
            String clientDocType,
            String clientDocNumber,
            String clientName,
            BigDecimal taxBase,
            BigDecimal taxIgv,
            BigDecimal total,
            String status,
            String origin,
            Long originId
    ) {
        this.emissionDate = emissionDate;
        this.docType = docType;
        this.docSeries = docSeries;
        this.docNumber = docNumber;
        this.clientDocType = clientDocType;
        this.clientDocNumber = clientDocNumber;
        this.clientName = clientName;
        this.taxBase = taxBase;
        this.taxIgv = taxIgv;
        this.total = total;
        this.status = status;
        this.origin = origin;
        this.originId = originId;
    }

    // Convenience method
    public String getFullDocumentNumber() {
        if (docSeries == null || docSeries.isBlank()) {
            return docNumber;
        }
        return docSeries + "-" + docNumber;
    }

    public LocalDate getEmissionDate() {
        return emissionDate;
    }

    public void setEmissionDate(LocalDate emissionDate) {
        this.emissionDate = emissionDate;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getDocSeries() {
        return docSeries;
    }

    public void setDocSeries(String docSeries) {
        this.docSeries = docSeries;
    }

    public String getDocNumber() {
        return docNumber;
    }

    public void setDocNumber(String docNumber) {
        this.docNumber = docNumber;
    }

    public String getClientDocType() {
        return clientDocType;
    }

    public void setClientDocType(String clientDocType) {
        this.clientDocType = clientDocType;
    }

    public String getClientDocNumber() {
        return clientDocNumber;
    }

    public void setClientDocNumber(String clientDocNumber) {
        this.clientDocNumber = clientDocNumber;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public BigDecimal getTaxBase() {
        return taxBase;
    }

    public void setTaxBase(BigDecimal taxBase) {
        this.taxBase = taxBase;
    }

    public BigDecimal getTaxIgv() {
        return taxIgv;
    }

    public void setTaxIgv(BigDecimal taxIgv) {
        this.taxIgv = taxIgv;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Long getOriginId() {
        return originId;
    }

    public void setOriginId(Long originId) {
        this.originId = originId;
    }
}
