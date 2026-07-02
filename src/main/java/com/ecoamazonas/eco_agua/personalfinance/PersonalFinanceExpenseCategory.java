package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceExpenseCategory {
    HOUSING("Vivienda"),
    UTILITIES("Servicios"),
    FOOD("Comida"),
    HEALTH("Salud"),
    STUDY("Estudios"),
    TRANSPORT("Transporte"),
    GYM("Gym / bienestar"),
    FAMILY("Familia"),
    MEMBERSHIP("Membresías"),
    OTHER("Otro");

    private final String label;

    PersonalFinanceExpenseCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
