package com.ecoamazonas.eco_agua.warehouse;

import com.ecoamazonas.eco_agua.inventory.InventoryMovement;
import com.ecoamazonas.eco_agua.inventory.InventoryMovementRepository;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ProductReorderSuggestionService {

    private static final String FILTER_NEEDS_REORDER = "NEEDS_REORDER";
    private static final String FILTER_OUT_OF_STOCK = "OUT_OF_STOCK";
    private static final String FILTER_LOW_STOCK = "LOW_STOCK";
    private static final String FILTER_AT_LIMIT = "AT_LIMIT";
    private static final String FILTER_ALL_CONFIGURED = "ALL_CONFIGURED";

    private final ProductService productService;
    private final InventoryMovementRepository movementRepository;

    public ProductReorderSuggestionService(
            ProductService productService,
            InventoryMovementRepository movementRepository
    ) {
        this.productService = productService;
        this.movementRepository = movementRepository;
    }

    @Transactional(readOnly = true)
    public ProductReorderSuggestionSnapshot buildSuggestions(String statusFilter) {
        String effectiveFilter = normalizeFilter(statusFilter);
        List<ProductReorderSuggestionRow> allRows = new ArrayList<>();

        int activeProducts = 0;
        int configuredMinimumProducts = 0;
        int outOfStockProducts = 0;
        int belowMinimumProducts = 0;
        int atLimitProducts = 0;
        int suggestedProducts = 0;

        for (Product product : productService.findAll()) {
            if (product == null || !product.isActive()) {
                continue;
            }

            activeProducts++;

            BigDecimal minimumStock = safe(product.getMinimumStock());
            if (minimumStock.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            configuredMinimumProducts++;

            BigDecimal currentStock = safe(product.getStock());
            StockStatus status = resolveStatus(currentStock, minimumStock);

            if ("OUT_OF_STOCK".equals(status.code())) {
                outOfStockProducts++;
            } else if ("LOW_STOCK".equals(status.code())) {
                belowMinimumProducts++;
            } else if ("AT_LIMIT".equals(status.code())) {
                atLimitProducts++;
            }

            BigDecimal suggestedQuantity = minimumStock.subtract(currentStock);
            if (suggestedQuantity.compareTo(BigDecimal.ZERO) < 0) {
                suggestedQuantity = BigDecimal.ZERO;
            }

            if (suggestedQuantity.compareTo(BigDecimal.ZERO) > 0) {
                suggestedProducts++;
            }

            InventoryMovement lastMovement = movementRepository
                    .findTopByProductOrderByMovementDateDescIdDesc(product)
                    .orElse(null);

            ProductReorderSuggestionRow row = new ProductReorderSuggestionRow(
                    product,
                    currentStock,
                    minimumStock,
                    suggestedQuantity,
                    status.code(),
                    status.label(),
                    status.cssClass(),
                    lastMovement != null ? lastMovement.getMovementDate() : null,
                    lastMovement != null
                            ? ProductReorderSuggestionRow.movementLabel(lastMovement.getMovementType())
                            : "Sin movimientos",
                    actionLabel(status.code(), suggestedQuantity),
                    status.priority()
            );

            if (matchesFilter(row, effectiveFilter)) {
                allRows.add(row);
            }
        }

        allRows.sort(
                Comparator.comparingInt(ProductReorderSuggestionRow::getPriority).reversed()
                        .thenComparing(ProductReorderSuggestionRow::getSuggestedQuantity, Comparator.reverseOrder())
                        .thenComparing(row -> row.getCategoryName() != null ? row.getCategoryName().toLowerCase() : "")
                        .thenComparing(row -> row.getProductName() != null ? row.getProductName().toLowerCase() : "")
        );

        ProductReorderSuggestionSummary summary = new ProductReorderSuggestionSummary(
                activeProducts,
                configuredMinimumProducts,
                outOfStockProducts,
                belowMinimumProducts,
                atLimitProducts,
                suggestedProducts
        );

        return new ProductReorderSuggestionSnapshot(summary, allRows);
    }

    private String normalizeFilter(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return FILTER_NEEDS_REORDER;
        }

        String value = statusFilter.trim().toUpperCase();
        return switch (value) {
            case FILTER_OUT_OF_STOCK, FILTER_LOW_STOCK, FILTER_AT_LIMIT, FILTER_ALL_CONFIGURED -> value;
            default -> FILTER_NEEDS_REORDER;
        };
    }

    private boolean matchesFilter(ProductReorderSuggestionRow row, String filter) {
        if (FILTER_ALL_CONFIGURED.equals(filter)) {
            return true;
        }
        if (FILTER_OUT_OF_STOCK.equals(filter)) {
            return row.isOutOfStock();
        }
        if (FILTER_LOW_STOCK.equals(filter)) {
            return row.isBelowMinimum();
        }
        if (FILTER_AT_LIMIT.equals(filter)) {
            return row.isAtLimit();
        }
        return row.isOutOfStock() || row.isBelowMinimum() || row.isAtLimit();
    }

    private StockStatus resolveStatus(BigDecimal currentStock, BigDecimal minimumStock) {
        if (currentStock.compareTo(BigDecimal.ZERO) <= 0) {
            return new StockStatus("OUT_OF_STOCK", "Agotado", "danger", 4);
        }
        if (currentStock.compareTo(minimumStock) < 0) {
            return new StockStatus("LOW_STOCK", "Bajo mínimo", "warning", 3);
        }
        if (currentStock.compareTo(minimumStock) == 0) {
            return new StockStatus("AT_LIMIT", "En el límite", "info", 2);
        }
        return new StockStatus("ENOUGH", "Suficiente", "success", 1);
    }

    private String actionLabel(String statusCode, BigDecimal suggestedQuantity) {
        if ("OUT_OF_STOCK".equals(statusCode)) {
            return "Comprar urgente";
        }
        if (suggestedQuantity != null && suggestedQuantity.compareTo(BigDecimal.ZERO) > 0) {
            return "Reponer stock";
        }
        if ("AT_LIMIT".equals(statusCode)) {
            return "Revisar antes de vender más";
        }
        return "Observar";
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private record StockStatus(String code, String label, String cssClass, int priority) {
    }
}
