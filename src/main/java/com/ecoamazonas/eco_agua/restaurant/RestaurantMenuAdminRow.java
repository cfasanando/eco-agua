package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record RestaurantMenuAdminRow(
        Long id,
        String name,
        String description,
        String imagePath,
        BigDecimal price,
        boolean active,
        boolean featured,
        BigDecimal stock,
        BigDecimal minimumStock,
        Long categoryId,
        String categoryName,
        boolean restaurantVisible,
        boolean restaurantAvailable,
        int restaurantSortOrder,
        BigDecimal recipeCost,
        int recipeItemCount,
        int recipeIssueCount,
        String stockControlMode,
        BigDecimal availablePortions,
        String limitingIngredient,
        int recipeLowIngredientCount
) {
    public BigDecimal safePrice() {
        return price == null ? BigDecimal.ZERO : price;
    }

    public BigDecimal safeStock() {
        return stock == null ? BigDecimal.ZERO : stock;
    }

    public BigDecimal safeMinimumStock() {
        return minimumStock == null ? BigDecimal.ZERO : minimumStock;
    }

    public BigDecimal safeAvailablePortions() {
        return availablePortions == null ? BigDecimal.ZERO : availablePortions.max(BigDecimal.ZERO);
    }

    public String imageOrFallback() {
        return imagePath == null || imagePath.isBlank() ? "/img/logo3-transparente.png" : imagePath;
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

    public String stockControlLabel() {
        return switch (safeStockControlMode()) {
            case "RECIPE" -> "Por receta";
            case "NONE" -> "Sin control";
            default -> "Por plato";
        };
    }

    public String stockControlBadge() {
        return switch (safeStockControlMode()) {
            case "RECIPE" -> "text-bg-info";
            case "NONE" -> "text-bg-secondary";
            default -> "text-bg-primary";
        };
    }

    public BigDecimal effectiveAvailableQuantity() {
        if (hasNoStockControl()) {
            return BigDecimal.valueOf(999999);
        }
        if (usesRecipeStock()) {
            return isRecipeComplete() ? safeAvailablePortions().setScale(0, RoundingMode.FLOOR) : BigDecimal.ZERO;
        }
        return safeStock().setScale(0, RoundingMode.FLOOR);
    }

    public String effectiveAvailableDisplay() {
        return hasNoStockControl() ? "Sin límite" : RestaurantDecimalFormat.quantity(effectiveAvailableQuantity());
    }

    public String limitingIngredientLabel() {
        return limitingIngredient == null || limitingIngredient.isBlank() ? "-" : limitingIngredient;
    }

    public String visibilityLabel() {
        return restaurantVisible ? "Visible" : "Oculto";
    }

    public String availabilityLabel() {
        if (!hasEffectiveStock()) {
            return usesRecipeStock() && !isRecipeComplete() ? "Receta incompleta" : "Agotado";
        }
        return restaurantAvailable ? "Disponible" : "Pausado";
    }

    public String availabilityBadge() {
        if (!hasEffectiveStock()) {
            return "text-bg-danger";
        }
        return restaurantAvailable ? "text-bg-success" : "text-bg-warning";
    }

    public String visibilityBadge() {
        return restaurantVisible ? "text-bg-primary" : "text-bg-secondary";
    }

    public boolean hasEffectiveStock() {
        return hasNoStockControl() || effectiveAvailableQuantity().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isOutOfStock() {
        return !hasEffectiveStock();
    }

    public boolean isLowStock() {
        if (hasNoStockControl() || isOutOfStock()) {
            return false;
        }
        if (usesRecipeStock()) {
            return recipeLowIngredientCount > 0 || safeAvailablePortions().compareTo(BigDecimal.valueOf(3)) <= 0;
        }
        return safeMinimumStock().compareTo(BigDecimal.ZERO) > 0
                && safeStock().compareTo(safeMinimumStock()) <= 0;
    }

    public String stockStatusLabel() {
        if (hasNoStockControl()) {
            return "Sin control";
        }
        if (isOutOfStock()) {
            return "Agotado";
        }
        if (isLowStock()) {
            return "Stock bajo";
        }
        return "Stock OK";
    }

    public String stockStatusBadge() {
        if (hasNoStockControl()) {
            return "text-bg-secondary";
        }
        if (isOutOfStock()) {
            return "text-bg-danger";
        }
        if (isLowStock()) {
            return "text-bg-warning";
        }
        return "text-bg-success";
    }

    public BigDecimal safeRecipeCost() {
        return recipeCost == null ? BigDecimal.ZERO : recipeCost;
    }

    public String priceDisplay() {
        return RestaurantDecimalFormat.money(safePrice());
    }

    public String stockDisplay() {
        return RestaurantDecimalFormat.quantity(safeStock());
    }

    public String minimumStockDisplay() {
        return RestaurantDecimalFormat.quantity(safeMinimumStock());
    }

    public String recipeCostDisplay() {
        return RestaurantDecimalFormat.money(safeRecipeCost());
    }

    public BigDecimal estimatedMarginAmount() {
        return safePrice().subtract(safeRecipeCost());
    }

    public BigDecimal estimatedMarginPercent() {
        if (safePrice().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return estimatedMarginAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(safePrice(), 2, RoundingMode.HALF_UP);
    }

    public String estimatedMarginAmountDisplay() {
        return RestaurantDecimalFormat.money(estimatedMarginAmount());
    }

    public String estimatedMarginPercentDisplay() {
        return RestaurantDecimalFormat.percentage(estimatedMarginPercent());
    }

    public boolean hasRecipe() {
        return recipeItemCount > 0;
    }

    public boolean isRecipeComplete() {
        return hasRecipe() && recipeIssueCount == 0;
    }

    public String recipeStatusLabel() {
        if (!hasRecipe()) {
            return "Sin receta";
        }
        return isRecipeComplete() ? "Receta completa" : "Receta incompleta";
    }

    public String recipeStatusBadge() {
        if (!hasRecipe()) {
            return "text-bg-secondary";
        }
        return isRecipeComplete() ? "text-bg-success" : "text-bg-warning";
    }

    public boolean canBeSold() {
        return active && restaurantVisible && restaurantAvailable && hasEffectiveStock();
    }
}
