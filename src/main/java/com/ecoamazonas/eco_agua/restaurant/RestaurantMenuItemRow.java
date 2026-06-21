package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record RestaurantMenuItemRow(
        Long id,
        String name,
        String description,
        String imagePath,
        BigDecimal price,
        boolean featured,
        BigDecimal stock,
        Long categoryId,
        String categoryName,
        boolean restaurantVisible,
        boolean restaurantAvailable,
        int restaurantSortOrder,
        String stockControlMode,
        int recipeItemCount,
        int recipeIssueCount,
        BigDecimal availablePortions,
        String limitingIngredient
) {
    public String imageOrFallback() {
        return imagePath == null || imagePath.isBlank() ? "/img/logo3-transparente.png" : imagePath;
    }

    public BigDecimal safePrice() {
        return price == null ? BigDecimal.ZERO : price;
    }

    public BigDecimal safeStock() {
        return stock == null ? BigDecimal.ZERO : stock;
    }

    public BigDecimal safeAvailablePortions() {
        return availablePortions == null ? BigDecimal.ZERO : availablePortions.max(BigDecimal.ZERO);
    }

    public String categoryLabel() {
        return categoryName == null || categoryName.isBlank() ? "Carta general" : categoryName;
    }

    public String safeStockControlMode() {
        String clean = stockControlMode == null ? "PRODUCT" : stockControlMode.trim().toUpperCase();
        return switch (clean) {
            case "RECIPE", "NONE" -> clean;
            default -> "PRODUCT";
        };
    }

    public boolean usesProductStock() {
        return "PRODUCT".equals(safeStockControlMode());
    }

    public boolean usesRecipeStock() {
        return "RECIPE".equals(safeStockControlMode());
    }

    public boolean hasNoStockControl() {
        return "NONE".equals(safeStockControlMode());
    }

    public boolean hasCompleteRecipe() {
        return recipeItemCount > 0 && recipeIssueCount == 0;
    }

    public BigDecimal effectiveAvailableQuantity() {
        if (hasNoStockControl()) {
            return BigDecimal.valueOf(999999);
        }
        if (usesRecipeStock()) {
            return hasCompleteRecipe() ? safeAvailablePortions().setScale(0, RoundingMode.FLOOR) : BigDecimal.ZERO;
        }
        return safeStock().setScale(0, RoundingMode.FLOOR);
    }

    public boolean hasEffectiveStock() {
        return effectiveAvailableQuantity().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isAvailableForSale() {
        return restaurantVisible && restaurantAvailable && hasEffectiveStock();
    }

    public String limitingIngredientLabel() {
        return limitingIngredient == null || limitingIngredient.isBlank() ? "-" : limitingIngredient;
    }
}
