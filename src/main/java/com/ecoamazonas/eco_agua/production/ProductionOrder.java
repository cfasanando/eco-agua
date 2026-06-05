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

    @Column(name = "batch_code", length = 50)
    private String batchCode;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    @Column(name = "quantity_expected", precision = 10, scale = 2, nullable = false)
    private BigDecimal quantityExpected = BigDecimal.ZERO;

    @Column(name = "quantity_produced", precision = 10, scale = 2, nullable = false)
    private BigDecimal quantityProduced = BigDecimal.ZERO;

    @Column(name = "quantity_loss", precision = 10, scale = 2, nullable = false)
    private BigDecimal quantityLoss = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ProductionStatus status = ProductionStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_status", length = 20, nullable = false)
    private ProductionQualityStatus qualityStatus = ProductionQualityStatus.PENDING;

    @Column(name = "quality_checked_at")
    private LocalDateTime qualityCheckedAt;

    @Column(name = "quality_checked_by", length = 120)
    private String qualityCheckedBy;

    @Column(name = "quality_cleaning_ok", nullable = false)
    private boolean qualityCleaningOk = false;

    @Column(name = "quality_packaging_ok", nullable = false)
    private boolean qualityPackagingOk = false;

    @Column(name = "quality_labeling_ok", nullable = false)
    private boolean qualityLabelingOk = false;

    @Column(name = "quality_product_ok", nullable = false)
    private boolean qualityProductOk = false;

    @Column(name = "quality_observation", length = 500)
    private String qualityObservation;

    @Column(name = "observation", length = 255)
    private String observation;

    @Column(name = "loss_reason", length = 255)
    private String lossReason;

    @Column(name = "total_input_cost", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalInputCost = BigDecimal.ZERO;

    @Column(name = "unit_cost_estimated", precision = 12, scale = 4, nullable = false)
    private BigDecimal unitCostEstimated = BigDecimal.ZERO;

    @Column(name = "real_unit_cost", precision = 12, scale = 4, nullable = false)
    private BigDecimal realUnitCost = BigDecimal.ZERO;

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
        normalizeQuantitiesAndCosts();
        if (status == null) status = ProductionStatus.DRAFT;
        if (qualityStatus == null) qualityStatus = ProductionQualityStatus.PENDING;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalizeQuantitiesAndCosts();
        if (qualityStatus == null) qualityStatus = ProductionQualityStatus.PENDING;
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

    @Transient
    public BigDecimal getLossRatePercent() {
        if (quantityExpected == null || quantityExpected.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal loss = quantityLoss != null ? quantityLoss : BigDecimal.ZERO;
        return loss.multiply(BigDecimal.valueOf(100)).divide(quantityExpected, 2, RoundingMode.HALF_UP);
    }

    public void refreshBatchCostFields() {
        normalizeQuantitiesAndCosts();
    }

    private void normalizeQuantitiesAndCosts() {
        if (quantityProduced == null) {
            quantityProduced = BigDecimal.ZERO;
        }
        quantityProduced = quantityProduced.setScale(2, RoundingMode.HALF_UP);

        if (quantityExpected == null || quantityExpected.compareTo(BigDecimal.ZERO) <= 0) {
            quantityExpected = quantityProduced;
        }
        quantityExpected = quantityExpected.setScale(2, RoundingMode.HALF_UP);

        BigDecimal calculatedLoss = quantityExpected.subtract(quantityProduced);
        if (calculatedLoss.compareTo(BigDecimal.ZERO) < 0) {
            calculatedLoss = BigDecimal.ZERO;
        }
        quantityLoss = calculatedLoss.setScale(2, RoundingMode.HALF_UP);

        if (totalInputCost == null) {
            totalInputCost = BigDecimal.ZERO;
        }
        totalInputCost = totalInputCost.setScale(2, RoundingMode.HALF_UP);

        unitCostEstimated = computeUnitCost(quantityExpected);
        realUnitCost = computeUnitCost(quantityProduced);
    }

    private BigDecimal computeUnitCost(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal total = totalInputCost != null ? totalInputCost : BigDecimal.ZERO;
        return total.divide(quantity, 4, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public LocalDate getProductionDate() { return productionDate; }
    public String getBatchCode() { return batchCode; }
    public Long getProductId() { return productId; }
    public Product getProduct() { return product; }
    public BigDecimal getQuantityExpected() { return quantityExpected; }
    public BigDecimal getQuantityProduced() { return quantityProduced; }
    public BigDecimal getQuantityLoss() { return quantityLoss; }
    public ProductionStatus getStatus() { return status; }
    public ProductionQualityStatus getQualityStatus() { return qualityStatus != null ? qualityStatus : ProductionQualityStatus.PENDING; }
    public LocalDateTime getQualityCheckedAt() { return qualityCheckedAt; }
    public String getQualityCheckedBy() { return qualityCheckedBy; }
    public boolean isQualityCleaningOk() { return qualityCleaningOk; }
    public boolean isQualityPackagingOk() { return qualityPackagingOk; }
    public boolean isQualityLabelingOk() { return qualityLabelingOk; }
    public boolean isQualityProductOk() { return qualityProductOk; }
    public String getQualityObservation() { return qualityObservation; }
    public String getObservation() { return observation; }
    public String getLossReason() { return lossReason; }
    public BigDecimal getTotalInputCost() { return totalInputCost; }
    public BigDecimal getUnitCostEstimated() { return unitCostEstimated; }
    public BigDecimal getRealUnitCost() { return realUnitCost; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<ProductionOrderSupply> getSupplies() { return supplies; }

    public void setId(Long id) { this.id = id; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    public void setProductId(Long productId) { this.productId = productId; }
    public void setProduct(Product product) { this.product = product; this.productId = product != null ? product.getId() : null; }
    public void setQuantityExpected(BigDecimal quantityExpected) { this.quantityExpected = quantityExpected; }
    public void setQuantityProduced(BigDecimal quantityProduced) { this.quantityProduced = quantityProduced; }
    public void setQuantityLoss(BigDecimal quantityLoss) { this.quantityLoss = quantityLoss; }
    public void setStatus(ProductionStatus status) { this.status = status; }
    public void setQualityStatus(ProductionQualityStatus qualityStatus) { this.qualityStatus = qualityStatus; }
    public void setQualityCheckedAt(LocalDateTime qualityCheckedAt) { this.qualityCheckedAt = qualityCheckedAt; }
    public void setQualityCheckedBy(String qualityCheckedBy) { this.qualityCheckedBy = qualityCheckedBy; }
    public void setQualityCleaningOk(boolean qualityCleaningOk) { this.qualityCleaningOk = qualityCleaningOk; }
    public void setQualityPackagingOk(boolean qualityPackagingOk) { this.qualityPackagingOk = qualityPackagingOk; }
    public void setQualityLabelingOk(boolean qualityLabelingOk) { this.qualityLabelingOk = qualityLabelingOk; }
    public void setQualityProductOk(boolean qualityProductOk) { this.qualityProductOk = qualityProductOk; }
    public void setQualityObservation(String qualityObservation) { this.qualityObservation = qualityObservation; }
    public void setObservation(String observation) { this.observation = observation; }
    public void setLossReason(String lossReason) { this.lossReason = lossReason; }
    public void setTotalInputCost(BigDecimal totalInputCost) { this.totalInputCost = totalInputCost; }
    public void setUnitCostEstimated(BigDecimal unitCostEstimated) { this.unitCostEstimated = unitCostEstimated; }
    public void setRealUnitCost(BigDecimal realUnitCost) { this.realUnitCost = realUnitCost; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setSupplies(List<ProductionOrderSupply> supplies) { this.supplies = supplies; }
}
