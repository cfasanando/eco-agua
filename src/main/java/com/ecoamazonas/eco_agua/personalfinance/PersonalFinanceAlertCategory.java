package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceAlertCategory {
    ALL("Todas", "bi-grid"),
    PAYMENT("Pagos", "bi-cash-coin"),
    INCOME("Ingresos", "bi-wallet2"),
    NEGOTIATION("Negociaciones", "bi-chat-left-text");

    private final String label;
    private final String icon;

    PersonalFinanceAlertCategory(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }
}
