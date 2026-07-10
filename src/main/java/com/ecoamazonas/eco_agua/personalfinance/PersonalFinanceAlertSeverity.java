package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceAlertSeverity {
    CRITICAL("Crítica", "danger", 0),
    WARNING("Atención", "warning", 1),
    INFO("Informativa", "info", 2);

    private final String label;
    private final String bootstrapClass;
    private final int order;

    PersonalFinanceAlertSeverity(String label, String bootstrapClass, int order) {
        this.label = label;
        this.bootstrapClass = bootstrapClass;
        this.order = order;
    }

    public String getLabel() {
        return label;
    }

    public String getBootstrapClass() {
        return bootstrapClass;
    }

    public int getOrder() {
        return order;
    }
}
