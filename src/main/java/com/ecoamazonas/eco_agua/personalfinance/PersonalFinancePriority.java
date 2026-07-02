package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinancePriority {
    CRITICAL("Crítico"),
    HIGH("Alto"),
    MEDIUM("Medio"),
    LOW("Bajo"),
    OPTIONAL("Opcional");

    private final String label;

    PersonalFinancePriority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
