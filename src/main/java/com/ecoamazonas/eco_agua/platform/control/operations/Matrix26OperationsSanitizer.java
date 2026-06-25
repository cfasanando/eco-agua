package com.ecoamazonas.eco_agua.platform.control.operations;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

final class Matrix26OperationsSanitizer {

    private static final Pattern SAFE_RUNTIME_NAME = Pattern.compile("[A-Za-z0-9._-]{1,120}");
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?i)(password|passwd|secret|token|api[_\\-.]?key|private[_\\-.]?key)(\\s*[:=]\\s*)([^\\s,;]+)"
    );
    private static final Pattern JDBC_CREDENTIALS = Pattern.compile(
            "(?i)(jdbc:[^\\s?]+\\?[^\\s]*?(?:password|user)=)([^&\\s]+)"
    );

    private Matrix26OperationsSanitizer() {
    }

    static boolean isSafeRuntimeName(String value) {
        return value != null && SAFE_RUNTIME_NAME.matcher(value.trim()).matches();
    }

    static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String sanitized = SECRET_ASSIGNMENT.matcher(value).replaceAll("$1$2***REDACTED***");
        sanitized = JDBC_CREDENTIALS.matcher(sanitized).replaceAll("$1***REDACTED***");
        return limit(sanitized.replace('\u0000', ' '), 1200);
    }

    static String slug(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return limit(normalized, 120);
    }

    static String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }
}
