package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceNegotiationStatus {
    NOT_STARTED("Sin iniciar"),
    PENDING_CONTACT("Pendiente de contactar"),
    IN_PROGRESS("En conversación"),
    PROPOSAL_RECEIVED("Propuesta recibida"),
    AGREEMENT_REACHED("Acuerdo alcanzado"),
    REPROGRAMMED("Reprogramada"),
    PAUSED("Pausada"),
    CLOSED("Cerrada");

    private final String label;

    PersonalFinanceNegotiationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
