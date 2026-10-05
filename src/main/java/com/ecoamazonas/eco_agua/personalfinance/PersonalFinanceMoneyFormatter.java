package com.ecoamazonas.eco_agua.personalfinance;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Component("personalFinanceMoneyFormatter")
public class PersonalFinanceMoneyFormatter {

    private static final DecimalFormatSymbols US_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US);

    public String money(BigDecimal value, PersonalFinanceCurrency currency) {
        return symbol(currency) + " " + amount(value);
    }

    public String moneyOrUndefined(BigDecimal value, boolean known, PersonalFinanceCurrency currency) {
        return known ? money(value, currency) : "No definido";
    }

    public String amount(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        DecimalFormat decimal = new DecimalFormat("#,##0.00", US_SYMBOLS);
        decimal.setRoundingMode(RoundingMode.HALF_UP);
        return decimal.format(safe);
    }

    public String symbol(PersonalFinanceCurrency currency) {
        if (currency == PersonalFinanceCurrency.USD) {
            return "US$";
        }
        return "S/";
    }

    public String shortLabel(PersonalFinanceCurrency currency) {
        if (currency == PersonalFinanceCurrency.USD) {
            return "Dólares";
        }
        return "Soles";
    }
}
