package com.ecoamazonas.eco_agua.restaurant;

import java.time.format.DateTimeFormatter;

/**
 * Presentation model for the cash-session list.
 *
 * Values are formatted before rendering so the Thymeleaf template does not
 * need chained method calls while the HTTP response is being streamed.
 */
public final class RestaurantCashSessionListItem {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final Long id;
    private final String businessDateText;
    private final String statusLabel;
    private final boolean open;
    private final String openedBy;
    private final String openedAtText;
    private final String openingAmountText;
    private final String expectedCashText;
    private final String closingAmountText;
    private final String differenceAmountText;
    private final String differenceCssClass;

    private RestaurantCashSessionListItem(Long id,
                                          String businessDateText,
                                          String statusLabel,
                                          boolean open,
                                          String openedBy,
                                          String openedAtText,
                                          String openingAmountText,
                                          String expectedCashText,
                                          String closingAmountText,
                                          String differenceAmountText,
                                          String differenceCssClass) {
        this.id = id;
        this.businessDateText = businessDateText;
        this.statusLabel = statusLabel;
        this.open = open;
        this.openedBy = openedBy;
        this.openedAtText = openedAtText;
        this.openingAmountText = openingAmountText;
        this.expectedCashText = expectedCashText;
        this.closingAmountText = closingAmountText;
        this.differenceAmountText = differenceAmountText;
        this.differenceCssClass = differenceCssClass;
    }

    public static RestaurantCashSessionListItem from(RestaurantCashSessionRow row) {
        if (row == null) {
            return null;
        }

        int differenceSign = row.safeDifferenceAmount().signum();
        String differenceClass = differenceSign < 0
                ? "text-danger"
                : differenceSign > 0 ? "text-success" : "";

        return new RestaurantCashSessionListItem(
                row.id(),
                row.businessDate() == null ? "-" : DATE_FORMAT.format(row.businessDate()),
                row.statusLabel(),
                row.isOpen(),
                row.openedBy() == null || row.openedBy().isBlank() ? "-" : row.openedBy(),
                row.openedAt() == null ? "-" : DATE_TIME_FORMAT.format(row.openedAt()),
                RestaurantDecimalFormat.money(row.safeOpeningAmount()),
                RestaurantDecimalFormat.money(row.safeExpectedCash()),
                RestaurantDecimalFormat.money(row.safeClosingAmount()),
                RestaurantDecimalFormat.money(row.safeDifferenceAmount()),
                differenceClass
        );
    }

    public Long getId() {
        return id;
    }

    public String getBusinessDateText() {
        return businessDateText;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public boolean isOpen() {
        return open;
    }

    public String getOpenedBy() {
        return openedBy;
    }

    public String getOpenedAtText() {
        return openedAtText;
    }

    public String getOpeningAmountText() {
        return openingAmountText;
    }

    public String getExpectedCashText() {
        return expectedCashText;
    }

    public String getClosingAmountText() {
        return closingAmountText;
    }

    public String getDifferenceAmountText() {
        return differenceAmountText;
    }

    public String getDifferenceCssClass() {
        return differenceCssClass;
    }
}
