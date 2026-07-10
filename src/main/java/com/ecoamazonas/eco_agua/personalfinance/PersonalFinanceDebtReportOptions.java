package com.ecoamazonas.eco_agua.personalfinance;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record PersonalFinanceDebtReportOptions(
        LocalDate cutoffDate,
        YearMonth startMonth,
        int months,
        PersonalFinanceDebtReportVersion version,
        boolean includeSchedules,
        boolean includePrivateNotes,
        boolean anonymizeContacts,
        Set<PersonalFinanceDebtReportSection> sections
) {
    public PersonalFinanceDebtReportOptions {
        cutoffDate = cutoffDate == null ? LocalDate.now() : cutoffDate;
        startMonth = startMonth == null ? YearMonth.from(cutoffDate) : startMonth;
        months = Math.max(1, Math.min(60, months));
        version = version == null ? PersonalFinanceDebtReportVersion.PRIVATE : version;
        if (sections == null || sections.isEmpty()) {
            sections = PersonalFinanceDebtReportSection.allSections();
        } else {
            sections = Set.copyOf(EnumSet.copyOf(sections));
        }
        if (version == PersonalFinanceDebtReportVersion.SHARED) {
            includePrivateNotes = false;
            anonymizeContacts = true;
        }
    }

    public boolean shared() {
        return version == PersonalFinanceDebtReportVersion.SHARED;
    }

    public LocalDate rangeEndDate() {
        return startMonth.plusMonths(months - 1L).atEndOfMonth();
    }

    public boolean sectionSelected(PersonalFinanceDebtReportSection section) {
        return section != null && sections.contains(section);
    }

    public boolean includesDebtContent() {
        return sections.stream().anyMatch(PersonalFinanceDebtReportSection::isDebtSection);
    }

    public boolean includesLivingCost() {
        return sectionSelected(PersonalFinanceDebtReportSection.LIVING_COST);
    }

    public boolean includesIncomeCapacity() {
        return sectionSelected(PersonalFinanceDebtReportSection.INCOME_CAPACITY);
    }

    public boolean includesDebtClassification(PersonalFinanceDebtClassification classification) {
        return sections.stream().anyMatch(section -> section.matches(classification));
    }

    public String sectionCodesCsv() {
        return sections.stream()
                .sorted()
                .map(PersonalFinanceDebtReportSection::getCode)
                .collect(Collectors.joining(","));
    }

    public String selectionSummary() {
        if (sections.size() == PersonalFinanceDebtReportSection.values().length) {
            return "Todas las categorías, costo de vida e ingresos";
        }
        return sections.stream()
                .sorted()
                .map(PersonalFinanceDebtReportSection::getLabel)
                .collect(Collectors.joining(" · "));
    }

    public String reportTitle() {
        if (sections.size() == PersonalFinanceDebtReportSection.values().length) {
            return "Informe integral de deudas";
        }
        if (sections.equals(Set.of(PersonalFinanceDebtReportSection.BANK_OWN, PersonalFinanceDebtReportSection.CREDIT_CARD))) {
            return "Informe de bancos propios";
        }
        if (sections.equals(Set.of(PersonalFinanceDebtReportSection.BANK_THIRD_PARTY, PersonalFinanceDebtReportSection.THIRD_PARTY_CONTRIBUTION))) {
            return "Informe de deudas por terceros";
        }
        if (sections.size() == 1) {
            PersonalFinanceDebtReportSection section = sections.iterator().next();
            return switch (section) {
                case LIVING_COST -> "Informe de costo de vida";
                case INCOME_CAPACITY -> "Informe de ingresos y capacidad de pago";
                case PRIVATE_LENDER -> "Informe de prestamistas con interés";
                case SAVINGS_CIRCLE -> "Informe de juntas";
                case BANK_OWN, CREDIT_CARD -> "Informe de bancos propios";
                case BANK_THIRD_PARTY, THIRD_PARTY_CONTRIBUTION -> "Informe de deudas por terceros";
                default -> "Informe de " + section.getLabel().toLowerCase(Locale.forLanguageTag("es-PE"));
            };
        }
        if (includesDebtContent() && !includesLivingCost() && !includesIncomeCapacity()) {
            return "Informe de deudas por categoría";
        }
        return "Informe financiero personalizado";
    }

    public String filenameSlug() {
        if (sections.size() == PersonalFinanceDebtReportSection.values().length) {
            return "integral";
        }
        if (sections.size() == 1) {
            return sections.iterator().next().getCode();
        }
        return "personalizado";
    }
}
