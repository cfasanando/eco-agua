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
        int restaurantSortOrder
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

    public boolean canBeSold() {
        return active && restaurantVisible && restaurantAvailable && !isOutOfStock();
    }
}
