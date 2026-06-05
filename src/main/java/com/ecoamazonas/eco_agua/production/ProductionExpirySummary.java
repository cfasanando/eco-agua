package com.ecoamazonas.eco_agua.production;

import java.time.LocalDate;

public class ProductionExpirySummary {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final long totalOrders;
    private final long withExpiryDate;
    private final long noExpiryDate;
    private final long validOrders;
    private final long expiringSoonOrders;
    private final long expiredOrders;

    public ProductionExpirySummary(
            LocalDate startDate,
            LocalDate endDate,
            long totalOrders,
            long withExpiryDate,
            long noExpiryDate,
            long validOrders,
            long expiringSoonOrders,
            long expiredOrders
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalOrders = totalOrders;
        this.withExpiryDate = withExpiryDate;
        this.noExpiryDate = noExpiryDate;
        this.validOrders = validOrders;
        this.expiringSoonOrders = expiringSoonOrders;
        this.expiredOrders = expiredOrders;
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public long getTotalOrders() { return totalOrders; }
    public long getWithExpiryDate() { return withExpiryDate; }
    public long getNoExpiryDate() { return noExpiryDate; }
    public long getValidOrders() { return validOrders; }
    public long getExpiringSoonOrders() { return expiringSoonOrders; }
    public long getExpiredOrders() { return expiredOrders; }
}
