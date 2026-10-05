package com.ecoamazonas.eco_agua.personalfinance;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class PersonalFinanceObligationVisibility {

    private PersonalFinanceObligationVisibility() {
    }

    static List<PersonalFinancePaymentObligation> withoutDuplicateDebtParents(
            List<PersonalFinancePaymentObligation> obligations
    ) {
        Set<Long> debtsWithScheduledObligation = new HashSet<>();
        for (PersonalFinancePaymentObligation obligation : obligations) {
            if (obligation == null || obligation.getSourceId() == null) {
                continue;
            }
            if (isScheduledDebtSource(obligation.getSourceType())) {
                debtsWithScheduledObligation.add(obligation.getSourceId());
            }
        }
        if (debtsWithScheduledObligation.isEmpty()) {
            return obligations;
        }
        return obligations.stream()
                .filter(obligation -> !isDuplicateDebtParent(obligation, debtsWithScheduledObligation))
                .toList();
    }

    private static boolean isDuplicateDebtParent(
            PersonalFinancePaymentObligation obligation,
            Set<Long> debtsWithScheduledObligation
    ) {
        return obligation != null
                && obligation.getSourceType() == PersonalFinanceObligationSourceType.DEBT
                && obligation.getSourceId() != null
                && debtsWithScheduledObligation.contains(obligation.getSourceId());
    }

    private static boolean isScheduledDebtSource(PersonalFinanceObligationSourceType sourceType) {
        return sourceType == PersonalFinanceObligationSourceType.DEBT_SCHEDULE
                || sourceType == PersonalFinanceObligationSourceType.PRIVATE_LENDER_INTEREST
                || sourceType == PersonalFinanceObligationSourceType.AUTO_DEDUCTION;
    }
}
