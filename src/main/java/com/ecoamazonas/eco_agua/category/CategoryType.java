package com.ecoamazonas.eco_agua.category;

import java.util.List;

/**
 * Category type used to group different business items.
 */
public enum CategoryType {
    PRODUCT,
    INCOME,

    /**
     * Legacy alias kept for old database rows.
     * Existing data may still contain EXPENSE.
     */
    EXPENSE,

    /**
     * Current canonical value for expense categories.
     */
    EXPENSES,

    SUPPLIER;

    public String getLabel() {
        return switch (this) {
            case PRODUCT -> "Producto";
            case INCOME -> "Ingreso";
            case EXPENSE, EXPENSES -> "Gasto";
            case SUPPLIER -> "Proveedor";
        };
    }

    /**
     * Returns true when this enum value should be shown in UI selectors.
     * Legacy aliases should stay hidden to avoid duplicated options.
     */
    public boolean isSelectable() {
        return this != EXPENSE;
    }

    /**
     * UI-safe values for dropdowns.
     */
    public static CategoryType[] selectableValues() {
        return new CategoryType[] {
                PRODUCT,
                INCOME,
                EXPENSES,
                SUPPLIER
        };
    }

    /**
     * Returns all enum values that should be treated as expense categories.
     */
    public static List<CategoryType> expenseTypes() {
        return List.of(EXPENSE, EXPENSES);
    }

    /**
     * Normalizes legacy and current expense values to the canonical one.
     */
    public CategoryType normalize() {
        if (this == EXPENSE) {
            return EXPENSES;
        }

        return this;
    }

    public boolean isExpenseType() {
        return this == EXPENSE || this == EXPENSES;
    }
}