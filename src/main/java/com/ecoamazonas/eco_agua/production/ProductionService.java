package com.ecoamazonas.eco_agua.production;

import com.ecoamazonas.eco_agua.inventory.InventoryMovementType;
import com.ecoamazonas.eco_agua.inventory.InventoryService;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import com.ecoamazonas.eco_agua.product.ProductSupply;
import com.ecoamazonas.eco_agua.supply.Supply;
import com.ecoamazonas.eco_agua.supply.SupplyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductionService {

    private final ProductionOrderRepository productionOrderRepository;
    private final ProductRepository productRepository;
    private final SupplyRepository supplyRepository;
    private final InventoryService inventoryService;

    public ProductionService(
            ProductionOrderRepository productionOrderRepository,
            ProductRepository productRepository,
            SupplyRepository supplyRepository,
            InventoryService inventoryService
    ) {
        this.productionOrderRepository = productionOrderRepository;
        this.productRepository = productRepository;
        this.supplyRepository = supplyRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<ProductionOrder> findByDateRange(LocalDate startDate, LocalDate endDate, ProductionStatus status) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);

        if (effectiveEnd.isBefore(effectiveStart)) {
            LocalDate tmp = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = tmp;
        }

        return productionOrderRepository.findByDateRangeAndStatus(effectiveStart, effectiveEnd, status);
    }

    @Transactional(readOnly = true)
    public ProductionOrder findDetailedById(Long id) {
        return productionOrderRepository.findDetailedById(id)
                .orElseThrow(() -> new IllegalArgumentException("Production order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Product> findActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<ProductionRecipeLine> buildRecipeLines(Long productId, BigDecimal quantityProduced) {
        if (productId == null) {
            throw new IllegalArgumentException("Product is required.");
        }

        BigDecimal effectiveQuantity = normalizeProducedQuantity(quantityProduced);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        List<ProductionRecipeLine> rows = new ArrayList<>();

        for (ProductSupply composition : product.getSuppliesComposition()) {
            if (composition == null || composition.getSupply() == null) {
                continue;
            }

            Supply supply = composition.getSupply();
            BigDecimal recipeQuantity = composition.getQuantityUsed() != null
                    ? composition.getQuantityUsed()
                    : BigDecimal.ZERO;

            BigDecimal calculatedQuantity = recipeQuantity.multiply(effectiveQuantity)
                    .setScale(4, RoundingMode.HALF_UP);

            BigDecimal unitCost = supply.getUnitCost() != null
                    ? supply.getUnitCost()
                    : BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

            BigDecimal lineTotal = calculatedQuantity.multiply(unitCost)
                    .setScale(2, RoundingMode.HALF_UP);

            rows.add(new ProductionRecipeLine(
                    supply.getId(),
                    supply.getName(),
                    supply.getUnit(),
                    recipeQuantity.setScale(4, RoundingMode.HALF_UP),
                    calculatedQuantity,
                    unitCost,
                    lineTotal
            ));
        }

        return rows;
    }

    @Transactional
    public ProductionOrder createDraft(
            LocalDate productionDate,
            Long productId,
            BigDecimal quantityProduced,
            String observation,
            List<Long> supplyIds,
            List<BigDecimal> quantitiesUsed
    ) {
        if (productId == null) {
            throw new IllegalArgumentException("Product is required.");
        }

        if (quantityProduced == null || quantityProduced.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Produced quantity must be greater than zero.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        ProductionOrder order = new ProductionOrder();
        order.setProductionDate(productionDate != null ? productionDate : LocalDate.now());
        order.setProduct(product);
        order.setQuantityProduced(quantityProduced.setScale(2, RoundingMode.HALF_UP));
        order.setStatus(ProductionStatus.DRAFT);
        order.setObservation(clean(observation));

        BigDecimal totalInputCost = BigDecimal.ZERO;
        List<ProductionOrderSupply> lines = buildManualLines(supplyIds, quantitiesUsed);

        if (lines.isEmpty()) {
            lines = buildLinesFromRecipe(product, quantityProduced);
        }

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Production must contain at least one supply line.");
        }

        for (ProductionOrderSupply line : lines) {
            totalInputCost = totalInputCost.add(line.getLineTotal());
            order.addSupplyLine(line);
        }

        order.setTotalInputCost(totalInputCost.setScale(2, RoundingMode.HALF_UP));
        order.setUnitCostEstimated(computeUnitCost(order.getQuantityProduced(), totalInputCost));

        return productionOrderRepository.save(order);
    }

    @Transactional
    public ProductionOrder confirm(Long productionOrderId) {
        ProductionOrder order = findDetailedById(productionOrderId);

        if (order.getStatus() == ProductionStatus.CONFIRMED) {
            return order;
        }

        if (order.getStatus() == ProductionStatus.CANCELED) {
            throw new IllegalArgumentException("Canceled production cannot be confirmed.");
        }

        if (order.getSupplies() == null || order.getSupplies().isEmpty()) {
            throw new IllegalArgumentException("Production must contain at least one supply line.");
        }

        for (ProductionOrderSupply line : order.getSupplies()) {
            Supply supply = line.getSupply();
            if (supply == null) {
                supply = supplyRepository.findById(line.getSupplyId())
                        .orElseThrow(() -> new IllegalArgumentException("Supply not found: " + line.getSupplyId()));
                line.setSupply(supply);
            }

            BigDecimal available = supply.getStock() != null ? supply.getStock() : BigDecimal.ZERO;
            BigDecimal required = line.getQuantityUsed() != null ? line.getQuantityUsed() : BigDecimal.ZERO;

            if (available.compareTo(required) < 0) {
                throw new IllegalArgumentException(
                        "Insufficient supply stock for: " + supply.getName()
                                + " (available: " + available + ", required: " + required + ")"
                );
            }
        }

        for (ProductionOrderSupply line : order.getSupplies()) {
            inventoryService.registerSupplyMovement(
                    line.getSupplyId(),
                    BigDecimal.ZERO,
                    line.getQuantityUsed(),
                    InventoryMovementType.PRODUCTION,
                    "PRODUCTION_ORDER",
                    order.getId(),
                    "Supply consumption for production #" + order.getId(),
                    order.getProductionDate()
            );
        }

        inventoryService.registerProductMovement(
                order.getProductId(),
                order.getQuantityProduced(),
                BigDecimal.ZERO,
                InventoryMovementType.PRODUCTION,
                "PRODUCTION_ORDER",
                order.getId(),
                "Finished product from production #" + order.getId(),
                order.getProductionDate()
        );

        order.setStatus(ProductionStatus.CONFIRMED);
        return productionOrderRepository.save(order);
    }

    @Transactional
    public ProductionOrder cancel(Long productionOrderId) {
        ProductionOrder order = findDetailedById(productionOrderId);

        if (order.getStatus() == ProductionStatus.CANCELED) {
            return order;
        }

        if (order.getStatus() == ProductionStatus.CONFIRMED) {
            for (ProductionOrderSupply line : order.getSupplies()) {
                inventoryService.registerSupplyMovement(
                        line.getSupplyId(),
                        line.getQuantityUsed(),
                        BigDecimal.ZERO,
                        InventoryMovementType.ADJUSTMENT,
                        "PRODUCTION_ORDER",
                        order.getId(),
                        "Reverse supply consumption from canceled production #" + order.getId(),
                        order.getProductionDate()
                );
            }

            inventoryService.registerProductMovement(
                    order.getProductId(),
                    BigDecimal.ZERO,
                    order.getQuantityProduced(),
                    InventoryMovementType.ADJUSTMENT,
                    "PRODUCTION_ORDER",
                    order.getId(),
                    "Reverse finished product from canceled production #" + order.getId(),
                    order.getProductionDate()
            );
        }

        order.setStatus(ProductionStatus.CANCELED);
        return productionOrderRepository.save(order);
    }

    @Transactional
    public void deleteDraft(Long productionOrderId) {
        ProductionOrder order = findDetailedById(productionOrderId);

        if (order.getStatus() != ProductionStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft production orders can be deleted.");
        }

        productionOrderRepository.delete(order);
    }

    private List<ProductionOrderSupply> buildManualLines(List<Long> supplyIds, List<BigDecimal> quantitiesUsed) {
        List<ProductionOrderSupply> lines = new ArrayList<>();

        if (supplyIds == null || quantitiesUsed == null) {
            return lines;
        }

        int max = Math.min(supplyIds.size(), quantitiesUsed.size());

        for (int i = 0; i < max; i++) {
            Long supplyId = supplyIds.get(i);
            BigDecimal quantityUsed = quantitiesUsed.get(i);

            if (supplyId == null || quantityUsed == null || quantityUsed.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Supply supply = supplyRepository.findById(supplyId)
                    .orElseThrow(() -> new IllegalArgumentException("Supply not found: " + supplyId));

            lines.add(buildSupplyLine(supply, quantityUsed));
        }

        return lines;
    }

    private List<ProductionOrderSupply> buildLinesFromRecipe(Product product, BigDecimal quantityProduced) {
        List<ProductionOrderSupply> lines = new ArrayList<>();
        BigDecimal effectiveQuantity = normalizeProducedQuantity(quantityProduced);

        for (ProductSupply composition : product.getSuppliesComposition()) {
            if (composition == null || composition.getSupply() == null) {
                continue;
            }

            BigDecimal recipeQuantity = composition.getQuantityUsed() != null
                    ? composition.getQuantityUsed()
                    : BigDecimal.ZERO;

            BigDecimal requiredQuantity = recipeQuantity.multiply(effectiveQuantity)
                    .setScale(4, RoundingMode.HALF_UP);

            if (requiredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            lines.add(buildSupplyLine(composition.getSupply(), requiredQuantity));
        }

        return lines;
    }

    private ProductionOrderSupply buildSupplyLine(Supply supply, BigDecimal quantityUsed) {
        ProductionOrderSupply line = new ProductionOrderSupply();
        line.setSupply(supply);
        line.setQuantityUsed(quantityUsed.setScale(4, RoundingMode.HALF_UP));

        BigDecimal unitCost = supply.getUnitCost() != null
                ? supply.getUnitCost()
                : BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

        line.setUnitCostSnapshot(unitCost);
        line.setLineTotal(quantityUsed.multiply(unitCost).setScale(2, RoundingMode.HALF_UP));

        return line;
    }

    private BigDecimal computeUnitCost(BigDecimal quantityProduced, BigDecimal totalInputCost) {
        if (quantityProduced == null || quantityProduced.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        return totalInputCost.divide(quantityProduced, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeProducedQuantity(BigDecimal quantityProduced) {
        if (quantityProduced == null || quantityProduced.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        }

        return quantityProduced.setScale(2, RoundingMode.HALF_UP);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
