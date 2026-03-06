package com.ecoamazonas.eco_agua.product;

import com.ecoamazonas.eco_agua.supply.Supply;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_supply")
public class ProductSupply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_id", nullable = false)
    private Supply supply;

    @Column(name = "quantity_used", precision = 10, scale = 4, nullable = false)
    private BigDecimal quantityUsed;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Supply getSupply() {
        return supply;
    }

    public void setSupply(Supply supply) {
        this.supply = supply;
    }

    public BigDecimal getQuantityUsed() {
        return quantityUsed;
    }

    public void setQuantityUsed(BigDecimal quantityUsed) {
        this.quantityUsed = quantityUsed;
    }

    /**
     * Cost contribution of this supply to the product:
     * unitCost * quantityUsed.
     */
    @Transient
    public BigDecimal getCostContribution() {
        if (supply == null || quantityUsed == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal unitCost = supply.getUnitCost();
        if (unitCost == null) {
            return BigDecimal.ZERO;
        }
        return unitCost.multiply(quantityUsed);
    }
}
