package com.ecoamazonas.eco_agua.restaurant;

public record RestaurantIngredientSummary(
        int totalIngredients,
        int activeIngredients,
        int lowStockIngredients,
        int outOfStockIngredients,
        int inactiveIngredients
) {
}
