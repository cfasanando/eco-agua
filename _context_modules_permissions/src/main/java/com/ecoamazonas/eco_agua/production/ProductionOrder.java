package com.ecoamazonas.eco_agua.production;

import com.ecoamazonas.eco_agua.product.Product;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "production_order")
public class ProductionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "production_date", nullable = false)
    private LocalDate productionDate;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    @Column(name = "quantity_produced", precision = 10, scale = 2, nullable = false)
    private BigDecimal quantityProduced = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ProductionStatus status = ProductionStatus.DRAFT;

    @Column(name = "observation", length = 255)
    private String observation;

    @Column(name = "total_input_cost", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalInputCost = BigDecimal.ZERO;

    @Column(name = "unit_cost_estimated", precision = 12, scale = 4, nullable = false)
    private BigDecimal unitCostEstimated = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "productionOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ProductionOrderSupply> supplies = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (productionDate == null) productionDate = LocalDate.now();
        if (quantityProduced == null) quantityProduced = BigDecimal.ZERO;
        if (status == null) status = ProductionStatus.DRAFT;
        if (totalInputCost == null) totalInputCost = BigDecimal.ZERO;
        if (unitCostEstimated == null) unitCostEstimated = BigDecimal.ZERO;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (quantityProduced == null) quantityProduced = BigDecimal.ZERO;
        if (totalInputCost == null) totalInputCost = BigDecimal.ZERO;
        if (unitCostEstimated == null) unitCostEstimated = BigDecimal.ZERO;
    }

    public void addSupplyLine(ProductionOrderSupply line) {
        if (line == null) return;
        line.setProductionOrder(this);
        supplies.add(line);
    }

    @Transient
    public BigDecimal getUnitCostSafe() {
        if (quantityProduced == null || quantityProduced.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal total = totalInputCost != null ? totalInputCost : BigDecimal.ZERO;
        return total.divide(quantityProduced, 4, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public LocalDate getProductionDate() { return productionDate; }
    public Long getProductId() { return productId; }
    public Product getProduct() { return product; }
    public BigDecimal getQuantityProduced() { return quantityProduced; }
    public ProductionStatus getStatus() { return status; }
    public String getObservation() { return observation; }
    public BigDecimal getTotalInputCost() { return totalInputCost; }
    public BigDecimal getUnitCostEstimated() { return unitCostEstimated; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<ProductionOrderSupply> getSupplies() { return supplies; }

    public void setId(Long id) { this.id = id; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
    public void setProductId(Long productId) { this.productId = productId; }
    public void setProduct(Product product) { this.product = product; this.productId = product != null ? product.getId() : null; }
    public void setQuantityProduced(BigDecimal quantityProduced) { this.quantityProduced = quantityProduced; }
    public void setStatus(ProductionStatus status) { this.status = status; }
    public void setObservation(String observation) { this.observation = observation; }
    public void setTotalInputCost(BigDecimal totalInputCost) { this.totalInputCost = totalInputCost; }
    public void setUnitCostEstimated(BigDecimal unitCostEstimated) { this.unitCostEstimated = unitCostEstimated; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setSupplies(List<ProductionOrderSupply> supplies) { this.supplies = supplies; }
}
