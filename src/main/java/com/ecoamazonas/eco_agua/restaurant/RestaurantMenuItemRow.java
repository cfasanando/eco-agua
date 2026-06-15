package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;

public record RestaurantMenuItemRow(
        Long id,
        String name,
        String description,
        String imagePath,
        BigDecimal price,
        boolean featured,
        BigDecimal stock
) {
    public String imageOrFallback() {
        return imagePath == null || imagePath.isBlank() ? "/img/logo3-transparente.png" : imagePath;
    }
}
