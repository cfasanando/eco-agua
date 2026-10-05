package com.ecoamazonas.eco_agua.personalfinance;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

@Component
public class PersonalFinanceDebtReportFormatter {

    private static final Locale ES_PE = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PersonalFinanceMoneyFormatter moneyFormatter;

    public PersonalFinanceDebtReportFormatter(PersonalFinanceMoneyFormatter moneyFormatter) {
        this.moneyFormatter = moneyFormatter;
    }

    public String money(BigDecimal value, PersonalFinanceCurrency currency) {
        return moneyFormatter.money(value, currency);
    }

    public String moneyOrUndefined(BigDecimal value, boolean known, PersonalFinanceCurrency currency) {
        return known ? money(value, currency) : "No definido";
    }

    public String percent(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return safe.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    public String date(LocalDate value) {
        return value == null ? "No definida" : value.format(DATE_FORMAT);
    }

    public String dateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMAT);
    }

    public String month(YearMonth value) {
        if (value == null) {
            return "";
        }
        String month = value.getMonth().getDisplayName(TextStyle.FULL, ES_PE);
        return Character.toUpperCase(month.charAt(0)) + month.substring(1) + " " + value.getYear();
    }
}
