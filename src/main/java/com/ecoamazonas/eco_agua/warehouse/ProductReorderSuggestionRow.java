package com.ecoamazonas.eco_agua.warehouse;

import com.ecoamazonas.eco_agua.inventory.InventoryMovementType;
import com.ecoamazonas.eco_agua.product.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductReorderSuggestionRow {

    private final Product product;
    private final BigDecimal currentStock;
    private final BigDecimal minimumStock;
    private final BigDecimal suggestedQuantity;
    private final String statusCode;
    private final String statusLabel;
    private final String statusClass;
    private final LocalDateTime lastMovementDate;
    private final String lastMovementLabel;
    private final String actionLabel;
    private final int priority;

    public ProductReorderSuggestionRow(
            Product product,
            BigDecimal currentStock,
            BigDecimal minimumStock,
            BigDecimal suggestedQuantity,
            String statusCode,
            String statusLabel,
            String statusClass,
            LocalDateTime lastMovementDate,
            String lastMovementLabel,
            String actionLabel,
            int priority
    ) {
        this.product = product;
        this.currentStock = currentStock != null ? currentStock : BigDecimal.ZERO;
        this.minimumStock = minimumStock != null ? minimumStock : BigDecimal.ZERO;
        this.suggestedQuantity = suggestedQuantity != null ? suggestedQuantity : BigDecimal.ZERO;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
        this.statusClass = statusClass;
        this.lastMovementDate = lastMovementDate;
        this.lastMovementLabel = lastMovementLabel;
        this.actionLabel = actionLabel;
        this.priority = priority;
    }

    public Product getProduct() {
        return product;
    }

    public BigDecimal getCurrentStock() {
        return currentStock;
    }

    public BigDecimal getMinimumStock() {
        return minimumStock;
    }

    public BigDecimal getSuggestedQuantity() {
        return suggestedQuantity;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getStatusClass() {
        return statusClass;
    }

    public LocalDateTime getLastMovementDate() {
        return lastMovementDate;
    }

    public String getLastMovementLabel() {
        return lastMovementLabel;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public int getPriority() {
        return priority;
    }

    public Long getProductId() {
        return product != null ? product.getId() : null;
    }

    public String getProductName() {
        return product != null ? product.getName() : "-";
    }

    public String getCategoryName() {
        return product != null && product.getCategory() != null ? product.getCategory().getName() : "-";
    }

    public boolean isOutOfStock() {
        return "OUT_OF_STOCK".equals(statusCode);
    }

    public boolean isBelowMinimum() {
        return "LOW_STOCK".equals(statusCode);
    }

    public boolean isAtLimit() {
        return "AT_LIMIT".equals(statusCode);
    }

    public boolean needsPurchase() {
        return suggestedQuantity.compareTo(BigDecimal.ZERO) > 0;
    }

    public static String movementLabel(InventoryMovementType type) {
        if (type == null) {
            return "Sin movimientos";
        }

        return switch (type) {
            case INITIAL -> "Stock inicial";
            case PURCHASE -> "Compra / ingreso";
            case SALE -> "Venta";
            case ADJUSTMENT -> "Ajuste";
            case LOSS -> "Merma / pérdida";
            case PRODUCTION -> "Producción";
            case RETURN -> "Devolución";
        };
    }
}
