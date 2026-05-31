package com.ecoamazonas.eco_agua.order;

import com.ecoamazonas.eco_agua.client.Client;
import com.ecoamazonas.eco_agua.delivery.DeliveryStatus;
import com.ecoamazonas.eco_agua.delivery.DeliveryZone;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sale_order")
public class SaleOrder {

    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.18");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OrderStatus status = OrderStatus.REQUESTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 20, nullable = false)
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "delivery_person", length = 200)
    private String deliveryPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_zone_id")
    private DeliveryZone deliveryZone;

    @Column(name = "delivery_order_index")
    private Integer deliveryOrderIndex;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "borrowed_bottles")
    private Integer borrowedBottles = 0;

    @Column(name = "containers_delivered", nullable = false)
    private Integer containersDelivered = 0;

    @Column(name = "containers_returned", nullable = false)
    private Integer containersReturned = 0;

    @Column(name = "comment", length = 255)
    private String comment;

    @Column(name = "delivery_observation", length = 255)
    private String deliveryObservation;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleOrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "saleOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("paymentDate ASC, id ASC")
    private List<SaleOrderPayment> payments = new ArrayList<>();

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getDeliveryPerson() {
        return deliveryPerson;
    }

    public void setDeliveryPerson(String deliveryPerson) {
        this.deliveryPerson = deliveryPerson;
    }

    public DeliveryZone getDeliveryZone() {
        return deliveryZone;
    }

    public void setDeliveryZone(DeliveryZone deliveryZone) {
        this.deliveryZone = deliveryZone;
    }

    public Integer getDeliveryOrderIndex() {
        return deliveryOrderIndex;
    }

    public void setDeliveryOrderIndex(Integer deliveryOrderIndex) {
        this.deliveryOrderIndex = deliveryOrderIndex;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Integer getBorrowedBottles() {
        return borrowedBottles;
    }

    public void setBorrowedBottles(Integer borrowedBottles) {
        this.borrowedBottles = borrowedBottles;
    }

    public Integer getContainersDelivered() {
        return containersDelivered;
    }

    public void setContainersDelivered(Integer containersDelivered) {
        this.containersDelivered = containersDelivered;
    }

    public Integer getContainersReturned() {
        return containersReturned;
    }

    public void setContainersReturned(Integer containersReturned) {
        this.containersReturned = containersReturned;
    }

    @Transient
    public int getNetContainerChange() {
        int delivered = containersDelivered != null ? containersDelivered : 0;
        int returned = containersReturned != null ? containersReturned : 0;
        return delivered - returned;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDeliveryObservation() {
        return deliveryObservation;
    }

    public void setDeliveryObservation(String deliveryObservation) {
        this.deliveryObservation = deliveryObservation;
    }

    public List<SaleOrderItem> getItems() {
        return items;
    }

    public void setItems(List<SaleOrderItem> items) {
        this.items = items;
    }

    public List<SaleOrderPayment> getPayments() {
        return payments;
    }

    public void setPayments(List<SaleOrderPayment> payments) {
        this.payments.clear();

        if (payments != null) {
            for (SaleOrderPayment payment : payments) {
                addPayment(payment);
            }
        }
    }

    public void addPayment(SaleOrderPayment payment) {
        if (payment == null) {
            return;
        }

        if (!this.payments.contains(payment)) {
            this.payments.add(payment);
        }

        payment.setSaleOrder(this);
    }

    public void removePayment(SaleOrderPayment payment) {
        if (payment == null) {
            return;
        }

        this.payments.remove(payment);
        payment.setSaleOrder(null);
    }

    public void addItem(SaleOrderItem item) {
        item.setOrder(this);
        this.items.add(item);
    }

    public void removeItem(SaleOrderItem item) {
        item.setOrder(null);
        this.items.remove(item);
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

    @Transient
    public BigDecimal getPaidAmount() {
        if (payments == null || payments.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return payments.stream()
                .map(SaleOrderPayment::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transient
    public BigDecimal getPendingAmount() {
        BigDecimal total = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        BigDecimal pending = total.subtract(getPaidAmount());

        if (pending.compareTo(BigDecimal.ZERO) < 0) {
            pending = BigDecimal.ZERO;
        }

        return pending.setScale(2, RoundingMode.HALF_UP);
    }

    @Transient
    public boolean isFullyPaid() {
        return getPendingAmount().compareTo(BigDecimal.ZERO) <= 0;
    }

    @Transient
    public boolean isOverdue() {
        return getOverdueDays() > 0;
    }

    @Transient
    public long getOverdueDays() {
        if (status != OrderStatus.CREDIT || dueDate == null || isFullyPaid()) {
            return 0;
        }

        if (!LocalDate.now().isAfter(dueDate)) {
            return 0;
        }

        return ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }

    @PrePersist
    @PreUpdate
    private void prepareOrder() {
        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }

        if (this.taxRate == null) {
            this.taxRate = DEFAULT_TAX_RATE;
        }

        if (this.deliveryStatus == null) {
            this.deliveryStatus = DeliveryStatus.PENDING;
        }

        if (this.containersDelivered == null) {
            this.containersDelivered = this.borrowedBottles != null && this.borrowedBottles > 0
                    ? this.borrowedBottles
                    : 0;
        }

        if (this.containersReturned == null) {
            this.containersReturned = 0;
        }

        this.borrowedBottles = getNetContainerChange();

        if (this.taxBase == null || this.taxIgv == null) {
            BigDecimal divisor = BigDecimal.ONE.add(this.taxRate);
            BigDecimal base = this.totalAmount.divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal igv = this.totalAmount.subtract(base);
            this.taxBase = base;
            this.taxIgv = igv;
        }

        if (this.status == OrderStatus.CREDIT && this.dueDate == null && this.orderDate != null) {
            this.dueDate = this.orderDate.plusDays(7);
        }
    }
}
