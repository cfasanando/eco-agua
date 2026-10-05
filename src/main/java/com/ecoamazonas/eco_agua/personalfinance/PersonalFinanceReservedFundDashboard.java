package com.ecoamazonas.eco_agua.personalfinance;

import java.util.List;

public record PersonalFinanceReservedFundDashboard(
        PersonalFinanceReservedFundSummary summary,
        List<PersonalFinanceReservedFundView> funds,
        List<PersonalFinanceReservedFundMovementView> movements,
        List<PersonalFinancePaymentObligation> obligations,
        List<PersonalFinanceDebt> debts,
        List<PersonalFinanceDebtNegotiation> negotiations,
        PersonalFinanceReservedFundForm form
) {
}
