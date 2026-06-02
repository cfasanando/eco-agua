package com.ecoamazonas.eco_agua.income;

import com.ecoamazonas.eco_agua.category.Category;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "other_income")
public class OtherIncome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "observation", length = 255)
    private String observation;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "income_date", nullable = false)
    private LocalDate incomeDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // === Fields for accounting registry ===

    @Column(name = "doc_type", length = 5)
    private String docType;

    @Column(name = "doc_series", length = 10)
    private String docSeries;

    @Column(name = "doc_number", length = 20)
    private String docNumber;

    @Column(name = "tax_base", precision = 10, scale = 2)
    private BigDecimal taxBase;

    @Column(name = "tax_igv", precision = 10, scale = 2)
    private BigDecimal taxIgv;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    // Default tax rate for non-taxable auxiliary income records.
    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.ZERO;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (incomeDate == null) {
            incomeDate = LocalDate.now();
        }

        calculateTaxesIfNeeded();
    }

    @PreUpdate
    public void preUpdate() {
        calculateTaxesIfNeeded();
    }

    // ---------- Automatic tax calculation ----------

    private void calculateTaxesIfNeeded() {
        if (this.amount == null) {
            this.amount = BigDecimal.ZERO;
        }

        if (this.taxRate == null) {
            this.taxRate = DEFAULT_TAX_RATE;
        }

        if (this.taxBase == null || this.taxIgv == null) {
            if (this.taxRate.compareTo(BigDecimal.ZERO) <= 0) {
                this.taxBase = this.amount.setScale(2, RoundingMode.HALF_UP);
                this.taxIgv = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                return;
            }

            BigDecimal divisor = BigDecimal.ONE.add(this.taxRate);
            BigDecimal base = this.amount.divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal igv = this.amount.subtract(base).setScale(2, RoundingMode.HALF_UP);

            this.taxBase = base;
            this.taxIgv = igv;
        }
    }

    // ---------- Getters and setters ----------


    public String getDocumentLabel() {
        boolean hasDocType = docType != null && !docType.isBlank();
        boolean hasSeries = docSeries != null && !docSeries.isBlank();
        boolean hasNumber = docNumber != null && !docNumber.isBlank();

        if (!hasDocType && !hasSeries && !hasNumber) {
            return "Sin documento";
        }

        StringBuilder label = new StringBuilder();
        if (hasDocType) {
            label.append(docType.trim());
        }
        if (hasSeries) {
            if (!label.isEmpty()) {
                label.append(" ");
            }
            label.append(docSeries.trim());
        }
        if (hasNumber) {
            if (!label.isEmpty()) {
                label.append("-");
            }
            label.append(docNumber.trim());
        }
        return label.toString();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getIncomeDate() {
        return incomeDate;
    }

    public void setIncomeDate(LocalDate incomeDate) {
        this.incomeDate = incomeDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }
}
