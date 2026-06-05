package com.ecoamazonas.eco_agua.production;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import com.ecoamazonas.eco_agua.product.ProductSupply;
import com.ecoamazonas.eco_agua.product.ProductSupplyRepository;
import com.ecoamazonas.eco_agua.supply.Supply;
import com.ecoamazonas.eco_agua.supply.SupplyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductionRecipeService {

    private final ProductRepository productRepository;
    private final ProductSupplyRepository productSupplyRepository;
    private final SupplyRepository supplyRepository;

    public ProductionRecipeService(
            ProductRepository productRepository,
            ProductSupplyRepository productSupplyRepository,
            SupplyRepository supplyRepository
    ) {
        this.productRepository = productRepository;
        this.productSupplyRepository = productSupplyRepository;
        this.supplyRepository = supplyRepository;
    }

    @Transactional(readOnly = true)
    public ProductionRecipeListSnapshot buildListSnapshot() {
        List<Product> products = productRepository.findByActiveTrueOrderByNameAsc();
        List<ProductionRecipeProductRow> rows = new ArrayList<>();

        int configuredRecipes = 0;
        BigDecimal totalConfiguredUnitCost = BigDecimal.ZERO;

        for (Product product : products) {
            List<ProductionRecipeSupplyRow> recipeRows = buildRows(product.getId());
            BigDecimal estimatedUnitCost = sumLineTotals(recipeRows);

            if (!recipeRows.isEmpty()) {
                configuredRecipes++;
                totalConfiguredUnitCost = totalConfiguredUnitCost.add(estimatedUnitCost);
            }

            rows.add(new ProductionRecipeProductRow(
                    product.getId(),
                    product.getName(),
                    valueOrZero(product.getStock()).setScale(2, RoundingMode.HALF_UP),
                    recipeRows.size(),
                    estimatedUnitCost
            ));
        }

        BigDecimal averageEstimatedUnitCost = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        if (configuredRecipes > 0) {
            averageEstimatedUnitCost = totalConfiguredUnitCost.divide(
                    BigDecimal.valueOf(configuredRecipes),
                    4,
                    RoundingMode.HALF_UP
            );
        }

        ProductionRecipeListSummary summary = new ProductionRecipeListSummary(
                rows.size(),
                configuredRecipes,
                Math.max(rows.size() - configuredRecipes, 0),
                averageEstimatedUnitCost
        );

        return new ProductionRecipeListSnapshot(summary, rows);
    }

    @Transactional(readOnly = true)
    public ProductionRecipeDetailSnapshot buildDetailSnapshot(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        List<ProductionRecipeSupplyRow> rows = buildRows(productId);
        List<Supply> availableSupplies = supplyRepository.findByActiveTrueOrderByNameAsc();

        return new ProductionRecipeDetailSnapshot(
                product.getId(),
                product.getName(),
                valueOrZero(product.getStock()).setScale(2, RoundingMode.HALF_UP),
                sumLineTotals(rows),
                rows,
                availableSupplies
        );
    }

    @Transactional
    public void saveRecipe(Long productId, List<Long> supplyIds, List<BigDecimal> quantitiesUsed) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        List<ProductSupply> existingRows = productSupplyRepository.findByProductIdOrderByIdAsc(product.getId());
        if (!existingRows.isEmpty()) {
            productSupplyRepository.deleteAll(existingRows);
        }

        Map<Long, BigDecimal> normalizedQuantities = normalizeQuantities(supplyIds, quantitiesUsed);
        if (normalizedQuantities.isEmpty()) {
            return;
        }

        List<ProductSupply> newRows = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : normalizedQuantities.entrySet()) {
            Supply supply = supplyRepository.findById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Supply not found: " + entry.getKey()));

            ProductSupply recipeLine = new ProductSupply();
            recipeLine.setProduct(product);
            recipeLine.setSupply(supply);
            recipeLine.setQuantityUsed(entry.getValue().setScale(4, RoundingMode.HALF_UP));
            newRows.add(recipeLine);
        }

        productSupplyRepository.saveAll(newRows);
    }

    private List<ProductionRecipeSupplyRow> buildRows(Long productId) {
        List<ProductSupply> recipeLines = productSupplyRepository.findByProductIdOrderByIdAsc(productId);
        List<ProductionRecipeSupplyRow> rows = new ArrayList<>();

        for (ProductSupply recipeLine : recipeLines) {
            if (recipeLine == null || recipeLine.getSupply() == null) {
                continue;
            }

            Supply supply = recipeLine.getSupply();
            BigDecimal quantityPerUnit = valueOrZero(recipeLine.getQuantityUsed()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal unitCost = valueOrZero(supply.getUnitCost()).setScale(6, RoundingMode.HALF_UP);
            BigDecimal lineTotal = quantityPerUnit.multiply(unitCost).setScale(4, RoundingMode.HALF_UP);

            rows.add(new ProductionRecipeSupplyRow(
                    supply.getId(),
                    supply.getName(),
                    supply.getUnit(),
                    quantityPerUnit,
                    unitCost,
                    lineTotal,
                    valueOrZero(supply.getStock()).setScale(4, RoundingMode.HALF_UP)
            ));
        }

        return rows;
    }

    private Map<Long, BigDecimal> normalizeQuantities(List<Long> supplyIds, List<BigDecimal> quantitiesUsed) {
        Map<Long, BigDecimal> normalized = new LinkedHashMap<>();

        if (supplyIds == null || quantitiesUsed == null) {
            return normalized;
        }

        int max = Math.min(supplyIds.size(), quantitiesUsed.size());
        for (int i = 0; i < max; i++) {
            Long supplyId = supplyIds.get(i);
            BigDecimal quantity = quantitiesUsed.get(i);

            if (supplyId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal current = normalized.getOrDefault(supplyId, BigDecimal.ZERO);
            normalized.put(supplyId, current.add(quantity));
        }

        return normalized;
    }

    private BigDecimal sumLineTotals(List<ProductionRecipeSupplyRow> rows) {
        BigDecimal total = BigDecimal.ZERO;
        if (rows == null) {
            return total.setScale(4, RoundingMode.HALF_UP);
        }

        for (ProductionRecipeSupplyRow row : rows) {
            total = total.add(valueOrZero(row.getLineTotal()));
        }

        return total.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
