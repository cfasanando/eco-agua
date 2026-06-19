package com.ecoamazonas.eco_agua.restaurant;

public record RestaurantPublicTableContext(
        Long id,
        String code,
        String name,
        String area,
        int seats,
        String status
) {
    public String displayName() {
        return name == null || name.isBlank() ? "Mesa " + id : name;
    }

    public String areaLabel() {
        return area == null || area.isBlank() ? "Salón" : area;
    }
}
