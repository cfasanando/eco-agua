package com.ecoamazonas.eco_agua.inventory;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.supply.Supply;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_movement")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_id")
    private Supply supply;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", length = 20, nullable = false)
    private InventoryMovementType movementType;

    @Column(name = "quantity_in", precision = 10, scale = 4, nullable = false)
    private BigDecimal quantityIn = BigDecimal.ZERO;

    @Column(name = "quantity_out", precision = 10, scale = 4, nullable = false)
    private BigDecimal quantityOut = BigDecimal.ZERO;

    @Column(name = "movement_date", nullable = false)
    private LocalDateTime movementDate = LocalDateTime.now();

    @Column(length = 255)
    private String observation;

    @Column(name = "reference_module", length = 50)
    private String referenceModule;

    @Column(name = "reference_id")
    private Long referenceId;

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public Supply getSupply() {
        return supply;
    }

    public InventoryMovementType getMovementType() {
        return movementType;
    }

    public BigDecimal getQuantityIn() {
        return quantityIn;
    }

    public BigDecimal getQuantityOut() {
        return quantityOut;
    }

    public LocalDateTime getMovementDate() {
        return movementDate;
    }

    public String getObservation() {
        return observation;
    }

    public String getReferenceModule() {
        return referenceModule;
    }

    public Long getReferenceId() {
        return referenceId;
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

    public void setMovementType(InventoryMovementType movementType) {
        this.movementType = movementType;
    }

    public void setQuantityIn(BigDecimal quantityIn) {
        this.quantityIn = quantityIn != null
                ? quantityIn.setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    public void setQuantityOut(BigDecimal quantityOut) {
        this.quantityOut = quantityOut != null
                ? quantityOut.setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    public void setMovementDate(LocalDateTime movementDate) {
        this.movementDate = movementDate;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public void setReferenceModule(String referenceModule) {
        this.referenceModule = referenceModule;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }
}
