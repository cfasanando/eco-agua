package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceIncomeType {
    SALARY("Sueldo"),
    BONUS("Gratificación / bono"),
    CTS("CTS"),
    VACATION("Vacaciones"),
    FREELANCE("Freelance"),
    BUSINESS("Negocio"),
    OTHER("Otro ingreso");

    private final String label;

    PersonalFinanceIncomeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
