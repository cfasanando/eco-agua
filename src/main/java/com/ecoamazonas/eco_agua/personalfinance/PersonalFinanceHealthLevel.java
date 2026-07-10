package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceHealthLevel {
    STABLE("Estable", "success"),
    TIGHT("Ajustado", "warning"),
    RED("En rojo", "danger"),
    CRITICAL("Crítico", "critical");

    private final String label;
    private final String cssClass;

    PersonalFinanceHealthLevel(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String getLabel() {
        return label;
    }

    public String getCssClass() {
        return cssClass;
    }
}
