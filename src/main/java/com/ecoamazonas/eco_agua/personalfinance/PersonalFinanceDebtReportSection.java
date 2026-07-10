package com.ecoamazonas.eco_agua.personalfinance;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public enum PersonalFinanceDebtReportSection {
    BANK_OWN("bank-own", "Bancos propios", PersonalFinanceDebtClassification.BANK_OWN),
    CREDIT_CARD("credit-card", "Tarjetas propias", PersonalFinanceDebtClassification.CREDIT_CARD),
    BANK_THIRD_PARTY("bank-third-party", "Bancos por tercero", PersonalFinanceDebtClassification.BANK_THIRD_PARTY),
    THIRD_PARTY_CONTRIBUTION("third-party-contribution", "Aportes por tercero", PersonalFinanceDebtClassification.THIRD_PARTY_CONTRIBUTION),
    PRIVATE_LENDER("private-lender", "Prestamistas con interés", PersonalFinanceDebtClassification.PRIVATE_LENDER),
    FAMILY_DIRECT("family-direct", "Familiares / deudas directas", PersonalFinanceDebtClassification.FAMILY_DIRECT),
    SAVINGS_CIRCLE("savings-circle", "Juntas", PersonalFinanceDebtClassification.SAVINGS_CIRCLE),
    RECURRING_COMMITMENT("recurring-commitment", "Compromisos mensuales", PersonalFinanceDebtClassification.RECURRING_COMMITMENT),
    OTHER_DEBTS("other-debts", "Otras deudas", PersonalFinanceDebtClassification.OTHER, PersonalFinanceDebtClassification.MANUAL_COMMITMENT),
    LIVING_COST("living-cost", "Costo de vida"),
    INCOME_CAPACITY("income-capacity", "Ingresos y capacidad de pago");

    private final String code;
    private final String label;
    private final Set<PersonalFinanceDebtClassification> classifications;

    PersonalFinanceDebtReportSection(
            String code,
            String label,
            PersonalFinanceDebtClassification... classifications
    ) {
        this.code = code;
        this.label = label;
        if (classifications.length == 0) {
            this.classifications = Set.of();
        } else {
            EnumSet<PersonalFinanceDebtClassification> values = EnumSet.noneOf(PersonalFinanceDebtClassification.class);
            values.addAll(List.of(classifications));
            this.classifications = Set.copyOf(values);
        }
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean isDebtSection() {
        return !classifications.isEmpty();
    }

    public boolean matches(PersonalFinanceDebtClassification classification) {
        return classification != null && classifications.contains(classification);
    }

    public static EnumSet<PersonalFinanceDebtReportSection> allSections() {
        return EnumSet.allOf(PersonalFinanceDebtReportSection.class);
    }

    public static EnumSet<PersonalFinanceDebtReportSection> parse(List<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return allSections();
        }
        List<String> tokens = new ArrayList<>();
        for (String rawValue : rawValues) {
            if (rawValue == null) {
                continue;
            }
            for (String token : rawValue.split(",")) {
                String normalized = token.trim().toLowerCase(Locale.ROOT);
                if (!normalized.isBlank()) {
                    tokens.add(normalized);
                }
            }
        }
        if (tokens.isEmpty() || tokens.contains("all")) {
            return allSections();
        }
        EnumSet<PersonalFinanceDebtReportSection> result = EnumSet.noneOf(PersonalFinanceDebtReportSection.class);
        for (PersonalFinanceDebtReportSection section : values()) {
            if (tokens.contains(section.code)) {
                result.add(section);
            }
        }
        return result.isEmpty() ? allSections() : result;
    }
}
