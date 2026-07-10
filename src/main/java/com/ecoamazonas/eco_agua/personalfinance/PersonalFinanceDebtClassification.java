package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceDebtClassification {
    BANK_OWN("Banco propio", "text-bg-primary"),
    BANK_THIRD_PARTY("Banco por tercero", "text-bg-info"),
    CREDIT_CARD("Tarjeta de crédito", "text-bg-info"),
    PRIVATE_LENDER("Prestamista con interés", "text-bg-warning"),
    FAMILY_DIRECT("Familiar / directa", "text-bg-success"),
    THIRD_PARTY_CONTRIBUTION("Aporte por tercero", "text-bg-secondary"),
    SAVINGS_CIRCLE("Junta", "text-bg-dark"),
    RECURRING_COMMITMENT("Compromiso mensual", "text-bg-secondary"),
    OTHER("Otra deuda", "text-bg-secondary"),
    MANUAL_COMMITMENT("Compromiso manual", "text-bg-light");

    private final String label;
    private final String badgeClass;

    PersonalFinanceDebtClassification(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public boolean isBankRelated() {
        return this == BANK_OWN || this == BANK_THIRD_PARTY || this == CREDIT_CARD;
    }

    public boolean isLender() {
        return this == PRIVATE_LENDER;
    }

    public boolean isDirect() {
        return this == FAMILY_DIRECT;
    }

    public boolean isCommitment() {
        return this == THIRD_PARTY_CONTRIBUTION || this == SAVINGS_CIRCLE || this == RECURRING_COMMITMENT;
    }
}
