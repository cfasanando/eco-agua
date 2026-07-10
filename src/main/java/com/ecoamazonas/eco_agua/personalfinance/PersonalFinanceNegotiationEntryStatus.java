package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceNegotiationEntryStatus {
    DRAFT("Borrador", false),
    CONTACT_PENDING("Contacto pendiente", false),
    CONTACTED("Contactado", false),
    PROPOSAL_SENT("Propuesta enviada", false),
    COUNTER_OFFER("Contrapropuesta", false),
    ACCEPTED("Aceptada", true),
    REJECTED("Rechazada", true),
    EXPIRED("Vencida", true),
    PAUSED("Pausada", false),
    CLOSED("Cerrada", true);

    private final String label;
    private final boolean terminal;

    PersonalFinanceNegotiationEntryStatus(String label, boolean terminal) {
        this.label = label;
        this.terminal = terminal;
    }

    public String getLabel() {
        return label;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean isAccepted() {
        return this == ACCEPTED;
    }
}
