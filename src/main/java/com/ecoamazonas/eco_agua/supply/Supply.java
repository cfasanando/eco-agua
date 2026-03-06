package com.ecoamazonas.eco_agua.supply;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "supply")
public class Supply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(name = "base_quantity", precision = 10, scale = 4, nullable = false)
    private BigDecimal baseQuantity;

    @Column(name = "base_cost", precision = 10, scale = 2, nullable = false)
    private BigDecimal baseCost;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal stock = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;
    
    @Column(name = "group_label", length = 100)
    private String groupLabel;

    // Getters & setters

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getBaseQuantity() {
        return baseQuantity;
    }

    public BigDecimal getBaseCost() {
        return baseCost;
    }

    public BigDecimal getStock() {
        return stock;
    }

    public boolean isActive() {
        return active;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setBaseQuantity(BigDecimal baseQuantity) {
        this.baseQuantity = baseQuantity;
    }

    public void setBaseCost(BigDecimal baseCost) {
        this.baseCost = baseCost;
    }

    public void setStock(BigDecimal stock) {
        this.stock = stock;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getGroupLabel() {
        return groupLabel;
    }

    public void setGroupLabel(String groupLabel) {
        this.groupLabel = groupLabel;
    }
    
    

    /**
     * Compute unit cost (baseCost / baseQuantity).
     */
    @Transient
    public BigDecimal getUnitCost() {
        if (baseCost == null || baseQuantity == null) {
            return null;
        }
        if (baseQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        // 6 decimal places to keep enough precision for composition
        return baseCost.divide(baseQuantity, 6, RoundingMode.HALF_UP);
    }
}
