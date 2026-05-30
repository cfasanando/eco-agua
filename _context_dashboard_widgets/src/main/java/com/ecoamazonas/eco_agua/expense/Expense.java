package com.ecoamazonas.eco_agua.expense;

import com.ecoamazonas.eco_agua.category.Category;
import com.ecoamazonas.eco_agua.supplier.Supplier;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expense")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    // Document fields (aligned with table columns)
    @Column(name = "doc_type", length = 5)
    private String docType;

    @Column(name = "doc_series", length = 10)
    private String docSeries;

    @Column(name = "doc_number", length = 20)
    private String docNumber;

    @Column(name = "doc_origin", length = 20, nullable = false)
    private String docOrigin = "SUPPLIER";

    @Column(name = "igv_credit_usable", nullable = false)
    private boolean igvCreditUsable = true;

    @Column(name = "observation", length = 255)
    private String observation;

    @Column(name = "voucher_number", length = 50)
    private String voucherNumber;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "tax_base", precision = 10, scale = 2, nullable = false)
    private BigDecimal taxBase = BigDecimal.ZERO;

    @Column(name = "tax_igv", precision = 10, scale = 2, nullable = false)
    private BigDecimal taxIgv = BigDecimal.ZERO;

    @Column(name = "tax_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal taxRate = new BigDecimal("18.00");

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 20, nullable = false)
    private ExpensePaymentType paymentType = ExpensePaymentType.CASH;

    @Column(name = "is_debt", nullable = false)
    private boolean debt;

    @Column(name = "paid_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ExpenseStatus status = ExpenseStatus.OPEN;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "expense",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ExpensePayment> payments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addPayment(ExpensePayment payment) {
        payment.setExpense(this);
        this.payments.add(payment);
    }

    public BigDecimal getBalance() {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (paidAmount == null) {
            return amount;
        }
        return amount.subtract(paidAmount);
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

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
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

    public String getDocOrigin() {
        return docOrigin;
    }

    public void setDocOrigin(String docOrigin) {
        this.docOrigin = docOrigin;
    }

    public boolean isIgvCreditUsable() {
        return igvCreditUsable;
    }

    public void setIgvCreditUsable(boolean igvCreditUsable) {
        this.igvCreditUsable = igvCreditUsable;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public String getVoucherNumber() {
        return voucherNumber;
    }

    public void setVoucherNumber(String voucherNumber) {
        this.voucherNumber = voucherNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public ExpensePaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(ExpensePaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public boolean isDebt() {
        return debt;
    }

    public void setDebt(boolean debt) {
        this.debt = debt;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public ExpenseStatus getStatus() {
        return status;
    }

    public void setStatus(ExpenseStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ExpensePayment> getPayments() {
        return payments;
    }

    public void setPayments(List<ExpensePayment> payments) {
        this.payments = payments;
    }
}
