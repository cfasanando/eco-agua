package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceFrequency {
    MONTHLY("Mensual"),
    WEEKLY("Semanal"),
    YEARLY("Anual");

    private final String label;

    PersonalFinanceFrequency(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
