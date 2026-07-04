package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceCollectionStatus {
    NONE("Sin gestión de cobranza"),
    INTERNAL_COLLECTION("Cobranza interna del acreedor"),
    EXTERNAL_COLLECTION("Cobranza externa"),
    LEGAL_COLLECTION("Cobranza legal"),
    REPORTED_CREDIT_BUREAU("Reportada en central de riesgo"),
    SETTLEMENT_OFFER("Con oferta de liquidación");

    private final String label;

    PersonalFinanceCollectionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
