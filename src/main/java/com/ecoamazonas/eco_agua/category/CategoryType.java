package com.ecoamazonas.eco_agua.category;

/**
 * Category type used to group different business items.
 */
public enum CategoryType {
    PRODUCT,
    INCOME,
    EXPENSES,
    SUPPLIER;

    public String getLabel() {
        // UI label in Spanish (you can tweak if needed)
        return switch (this) {
            case PRODUCT -> "Producto";
            case INCOME -> "Ingreso";
            case EXPENSES -> "Gasto";
            case SUPPLIER -> "Proveedor";
        };
    }
}
