package com.ecoamazonas.eco_agua.order;

import com.ecoamazonas.eco_agua.product.Product;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_order_item")
public class SaleOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private SaleOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "description", length = 255, nullable = false)
    private String description;

    @Column(name = "quantity", precision = 10, scale = 2, nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total", precision = 10, scale = 2, nullable = false)
    private BigDecimal total;

    // ---------- Getters & setters ----------

    public Long getId() {
        return id;
    }

    public SaleOrder getOrder() {
        return order;
    }

    public Product getProduct() {
        return product;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOrder(SaleOrder order) {
        this.order = order;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
