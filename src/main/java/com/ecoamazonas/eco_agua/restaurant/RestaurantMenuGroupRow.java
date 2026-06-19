package com.ecoamazonas.eco_agua.restaurant;

import java.util.List;

public record RestaurantMenuGroupRow(
        String categoryName,
        List<RestaurantMenuItemRow> items
) {
    public int itemCount() {
        return items == null ? 0 : items.size();
    }
}
