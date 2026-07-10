package com.ecoamazonas.eco_agua.personalfinance;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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

    public String money(BigDecimal value, PersonalFinanceCurrency currency) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat decimal = new DecimalFormat("#,##0.00", symbols);
        decimal.setRoundingMode(RoundingMode.HALF_UP);
        String prefix = currency == PersonalFinanceCurrency.USD ? "US$ " : "S/ ";
        return prefix + decimal.format(safe);
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
