package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;

public record RestaurantDashboardSummary(
        int totalTables,
        int freeTables,
        int occupiedTables,
        int reservedTables,
        int activeOrders,
        int kitchenPendingOrders,
        int readyOrders,
        BigDecimal todaySales
) {
}
