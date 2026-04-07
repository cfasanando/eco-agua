package com.ecoamazonas.eco_agua.production;

import com.ecoamazonas.eco_agua.supply.Supply;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "production_order_supply")
public class ProductionOrderSupply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;

    @Column(name = "supply_id", nullable = false)
    private Long supplyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_id", insertable = false, updatable = false)
    private Supply supply;

    @Column(name = "quantity_used", precision = 10, scale = 4, nullable = false)
    private BigDecimal quantityUsed = BigDecimal.ZERO;

    @Column(name = "unit_cost_snapshot", precision = 12, scale = 6, nullable = false)
    private BigDecimal unitCostSnapshot = BigDecimal.ZERO;

    @Column(name = "line_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @PrePersist
    @PreUpdate
    protected void normalize() {
        if (quantityUsed == null) {
            quantityUsed = BigDecimal.ZERO;
        }

        if (unitCostSnapshot == null) {
            unitCostSnapshot = BigDecimal.ZERO;
        }

        lineTotal = quantityUsed.multiply(unitCostSnapshot).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    @Transient
    public Long getProductionOrderId() {
        return productionOrder != null ? productionOrder.getId() : null;
    }

    public ProductionOrder getProductionOrder() {
        return productionOrder;
    }

    public Long getSupplyId() {
        return supplyId;
    }

    public Supply getSupply() {
        return supply;
    }

    public BigDecimal getQuantityUsed() {
        return quantityUsed;
    }

    public BigDecimal getUnitCostSnapshot() {
        return unitCostSnapshot;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setProductionOrder(ProductionOrder productionOrder) {
        this.productionOrder = productionOrder;
    }

    public void setSupplyId(Long supplyId) {
        this.supplyId = supplyId;
    }

    public void setSupply(Supply supply) {
        this.supply = supply;
        this.supplyId = supply != null ? supply.getId() : null;
    }

    public void setQuantityUsed(BigDecimal quantityUsed) {
        this.quantityUsed = quantityUsed;
    }

    public void setUnitCostSnapshot(BigDecimal unitCostSnapshot) {
        this.unitCostSnapshot = unitCostSnapshot;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
