package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;

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
        int recipeIssueCount
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

    public String imageOrFallback() {
        return imagePath == null || imagePath.isBlank() ? "/img/logo3-transparente.png" : imagePath;
    }

    public String categoryLabel() {
        return categoryName == null || categoryName.isBlank() ? "Carta general" : categoryName;
    }

    public String visibilityLabel() {
        return restaurantVisible ? "Visible" : "Oculto";
    }

    public String availabilityLabel() {
        if (isOutOfStock()) {
            return "Agotado";
        }
        return restaurantAvailable ? "Disponible" : "Pausado";
    }

    public String availabilityBadge() {
        if (isOutOfStock()) {
            return "text-bg-danger";
        }
        return restaurantAvailable ? "text-bg-success" : "text-bg-warning";
    }

    public String visibilityBadge() {
        return restaurantVisible ? "text-bg-primary" : "text-bg-secondary";
    }

    public boolean isOutOfStock() {
        return safeStock().compareTo(BigDecimal.ZERO) <= 0;
    }

    public boolean isLowStock() {
        return !isOutOfStock()
                && safeMinimumStock().compareTo(BigDecimal.ZERO) > 0
                && safeStock().compareTo(safeMinimumStock()) <= 0;
    }

    public String stockStatusLabel() {
        if (isOutOfStock()) {
            return "Agotado";
        }
        if (isLowStock()) {
            return "Stock bajo";
        }
        return "Stock OK";
    }

    public String stockStatusBadge() {
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
                .divide(safePrice(), 2, java.math.RoundingMode.HALF_UP);
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
        return active && restaurantVisible && restaurantAvailable && !isOutOfStock();
    }
}
