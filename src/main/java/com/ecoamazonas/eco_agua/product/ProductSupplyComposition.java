package com.ecoamazonas.eco_agua.product;

import com.ecoamazonas.eco_agua.supply.Supply;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_supply_composition")
public class ProductSupplyComposition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_id", nullable = false)
    private Supply supply;

    @Column(name = "quantity", precision = 10, scale = 4, nullable = false)
    private BigDecimal quantity;

    // Getters & setters

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public Supply getSupply() {
        return supply;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setSupply(Supply supply) {
        this.supply = supply;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
