package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;

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
        int restaurantSortOrder
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

    public String categoryLabel() {
        return categoryName == null || categoryName.isBlank() ? "Carta general" : categoryName;
    }

    public boolean isAvailableForSale() {
        return restaurantVisible && restaurantAvailable;
    }
}
