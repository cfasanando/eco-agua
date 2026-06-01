package com.ecoamazonas.eco_agua.warehouse;

import com.ecoamazonas.eco_agua.inventory.InventoryMovement;
import com.ecoamazonas.eco_agua.inventory.InventoryMovementType;

import java.math.BigDecimal;

public class ProductKardexRow {

    private final int rowNumber;
    private final InventoryMovement movement;
    private final BigDecimal balanceAfter;

    public ProductKardexRow(int rowNumber, InventoryMovement movement, BigDecimal balanceAfter) {
        this.rowNumber = rowNumber;
        this.movement = movement;
        this.balanceAfter = balanceAfter != null ? balanceAfter : BigDecimal.ZERO;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public InventoryMovement getMovement() {
        return movement;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public BigDecimal getQuantityIn() {
        if (movement == null || movement.getQuantityIn() == null) {
            return BigDecimal.ZERO;
        }
        return movement.getQuantityIn();
    }

    public BigDecimal getQuantityOut() {
        if (movement == null || movement.getQuantityOut() == null) {
            return BigDecimal.ZERO;
        }
        return movement.getQuantityOut();
    }

    public BigDecimal getNetQuantity() {
        return getQuantityIn().subtract(getQuantityOut());
    }

    public String getMovementTypeLabel() {
        if (movement == null || movement.getMovementType() == null) {
            return "Movimiento";
        }

        InventoryMovementType type = movement.getMovementType();
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

    public String getDirectionLabel() {
        boolean hasIn = getQuantityIn().compareTo(BigDecimal.ZERO) > 0;
        boolean hasOut = getQuantityOut().compareTo(BigDecimal.ZERO) > 0;

        if (hasIn && hasOut) {
            return "Mixto";
        }
        if (hasIn) {
            return "Entrada";
        }
        if (hasOut) {
            return "Salida";
        }
        return "Sin cantidad";
    }

    public String getDirectionCode() {
        boolean hasIn = getQuantityIn().compareTo(BigDecimal.ZERO) > 0;
        boolean hasOut = getQuantityOut().compareTo(BigDecimal.ZERO) > 0;

        if (hasIn && hasOut) {
            return "MIXED";
        }
        if (hasIn) {
            return "IN";
        }
        if (hasOut) {
            return "OUT";
        }
        return "NONE";
    }

    public String getReferenceLabel() {
        if (movement == null || movement.getReferenceModule() == null || movement.getReferenceModule().isBlank()) {
            return "Sin referencia";
        }

        String module = movement.getReferenceModule().trim();
        Long referenceId = movement.getReferenceId();

        if ("SALE_ORDER".equalsIgnoreCase(module)) {
            return referenceId != null ? "Pedido #" + referenceId : "Pedido";
        }
        if ("MANUAL".equalsIgnoreCase(module)) {
            return "Ajuste manual";
        }
        if ("INITIAL".equalsIgnoreCase(module)) {
            return "Stock inicial";
        }
        if ("LOSS".equalsIgnoreCase(module)) {
            return "Merma / pérdida";
        }
        if ("RETURN".equalsIgnoreCase(module)) {
            return "Devolución";
        }
        if ("PRODUCTION".equalsIgnoreCase(module)) {
            return referenceId != null ? "Producción #" + referenceId : "Producción";
        }
        if ("PURCHASE".equalsIgnoreCase(module)) {
            return referenceId != null ? "Compra #" + referenceId : "Compra";
        }

        return referenceId != null ? module + " #" + referenceId : module;
    }

    public String getReferenceUrl() {
        if (movement == null || movement.getReferenceModule() == null || movement.getReferenceId() == null) {
            return null;
        }

        String module = movement.getReferenceModule().trim();
        if ("SALE_ORDER".equalsIgnoreCase(module)) {
            return "/orders/" + movement.getReferenceId();
        }

        return null;
    }
}
