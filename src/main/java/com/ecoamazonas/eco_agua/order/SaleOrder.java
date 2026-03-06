package com.ecoamazonas.eco_agua.order;

import com.ecoamazonas.eco_agua.client.Client;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sale_order")
public class SaleOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OrderStatus status = OrderStatus.REQUESTED;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "delivery_person", length = 200)
    private String deliveryPerson;

    @Column(name = "borrowed_bottles")
    private Integer borrowedBottles = 0;

    @Column(name = "comment", length = 255)
    private String comment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleOrderItem> items = new ArrayList<>();

    // === Fields for accounting registry ===

    @Column(name = "doc_type", length = 5)
    private String docType; // FA, BO, TK, SC

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

    // === Default IGV (VAT) rate ===
    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.18");

    // ---------- Getters & setters ----------

    public Long getId() {
        return id;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public Client getClient() {
        return client;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getDeliveryPerson() {
        return deliveryPerson;
    }

    public Integer getBorrowedBottles() {
        return borrowedBottles;
    }

    public String getComment() {
        return comment;
    }

    public List<SaleOrderItem> getItems() {
        return items;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setDeliveryPerson(String deliveryPerson) {
        this.deliveryPerson = deliveryPerson;
    }

    public void setBorrowedBottles(Integer borrowedBottles) {
        this.borrowedBottles = borrowedBottles;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setItems(List<SaleOrderItem> items) {
        this.items = items;
    }

    // Helper to keep bidirectional relation consistent
    public void addItem(SaleOrderItem item) {
        item.setOrder(this);
        this.items.add(item);
    }

    public void removeItem(SaleOrderItem item) {
        item.setOrder(null);
        this.items.remove(item);
    }

    // ---------- Getters & setters for accounting ----------

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

    // ---------- Automatic tax calculation ----------

    @PrePersist
    @PreUpdate
    private void calculateTaxesIfNeeded() {
        System.out.println(">>> SaleOrder.calculateTaxesIfNeeded() BEFORE: total=" + totalAmount
                + ", taxBase=" + taxBase + ", taxIgv=" + taxIgv + ", rate=" + taxRate);

        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }

        if (this.taxRate == null) {
            this.taxRate = DEFAULT_TAX_RATE;
        }

        if (this.taxBase == null || this.taxIgv == null) {
            BigDecimal divisor = BigDecimal.ONE.add(this.taxRate); // e.g. 1.18
            BigDecimal base = this.totalAmount
                    .divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal igv = this.totalAmount.subtract(base);

            this.taxBase = base;
            this.taxIgv = igv;
        }

        System.out.println(">>> SaleOrder.calculateTaxesIfNeeded() AFTER: total=" + totalAmount
                + ", taxBase=" + taxBase + ", taxIgv=" + taxIgv + ", rate=" + taxRate);
    }
}
