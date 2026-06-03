package com.ecoamazonas.eco_agua.accounting.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

final class AccountingCsvExportHelper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String SEPARATOR = ";";

    private AccountingCsvExportHelper() {
    }

    static ResponseEntity<byte[]> csv(String filename, StringBuilder content) {
        byte[] body = ("\uFEFF" + content).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }

    static void row(StringBuilder builder, Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(escape(values[i]));
        }
        builder.append('\n');
    }

    static String date(LocalDate value) {
        return value == null ? "" : value.format(DATE_FORMATTER);
    }

    static String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String escape(Object rawValue) {
        String value = text(rawValue);
        boolean quote = value.contains(SEPARATOR) || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return quote ? "\"" + escaped + "\"" : escaped;
    }
}
