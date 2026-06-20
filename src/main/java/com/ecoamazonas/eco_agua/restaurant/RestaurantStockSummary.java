package com.ecoamazonas.eco_agua.restaurant;

public record RestaurantStockSummary(
        int totalItems,
        int availableItems,
        int lowStockItems,
        int outOfStockItems,
        int hiddenItems
) {
}
