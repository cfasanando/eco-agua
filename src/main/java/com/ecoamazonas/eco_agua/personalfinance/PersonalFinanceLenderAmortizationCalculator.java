package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class PersonalFinanceLenderAmortizationCalculator {

    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 10;

    private PersonalFinanceLenderAmortizationCalculator() {
    }

    public static List<PersonalFinanceLenderInstallment> calculate(
            BigDecimal openingBalance,
            BigDecimal monthlyRatePercent,
            int installmentCount
    ) {
        BigDecimal balance = money(openingBalance);
        BigDecimal ratePercent = monthlyRatePercent == null ? BigDecimal.ZERO : monthlyRatePercent;

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El saldo pendiente debe ser mayor que cero.");
        }
        if (ratePercent.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El interés mensual no puede ser negativo.");
        }
        if (installmentCount < 1 || installmentCount > 60) {
            throw new IllegalArgumentException("El número de cuotas debe estar entre 1 y 60.");
        }

        BigDecimal rate = ratePercent.divide(new BigDecimal("100"), RATE_SCALE, RoundingMode.HALF_UP);
        BigDecimal fixedPrincipal = balance.divide(BigDecimal.valueOf(installmentCount), MONEY_SCALE, RoundingMode.HALF_UP);
        List<PersonalFinanceLenderInstallment> installments = new ArrayList<>(installmentCount);

        for (int index = 1; index <= installmentCount; index++) {
            BigDecimal opening = balance;
            BigDecimal principal = index == installmentCount
                    ? opening
                    : fixedPrincipal.min(opening);
            BigDecimal interest = money(opening.multiply(rate));
            BigDecimal total = money(principal.add(interest));
            BigDecimal closing = money(opening.subtract(principal));

            installments.add(new PersonalFinanceLenderInstallment(
                    index,
                    opening,
                    principal,
                    interest,
                    total,
                    closing
            ));
            balance = closing;
        }

        return List.copyOf(installments);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
