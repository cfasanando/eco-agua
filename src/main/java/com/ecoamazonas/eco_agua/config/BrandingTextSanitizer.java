package com.ecoamazonas.eco_agua.config;

public final class BrandingTextSanitizer {

    private BrandingTextSanitizer() {
    }

    public static String clean(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return normalized;
        }

        return normalized
                .replace("\u00C3\u00A1", "á")
                .replace("\u00C3\u00A9", "é")
                .replace("\u00C3\u00AD", "í")
                .replace("\u00C3\u00B3", "ó")
                .replace("\u00C3\u00BA", "ú")
                .replace("\u00C3\u00B1", "ñ")
                .replace("\u00C3\u0081", "Á")
                .replace("\u00C3\u0089", "É")
                .replace("\u00C3\u008D", "Í")
                .replace("\u00C3\u0093", "Ó")
                .replace("\u00C3\u009A", "Ú")
                .replace("\u00C3\u0091", "Ñ")
                .replace("\u00C2\u00BF", "¿")
                .replace("\u00C2\u00A1", "¡")
                .replace("\u00C2\u00B0", "°")
                .replace("\u00E2\u0080\u0093", "–")
                .replace("\u00E2\u0080\u0094", "—")
                .replace("\u00E2\u0080\u009C", "“")
                .replace("\u00E2\u0080\u009D", "”")
                .replace("\u00E2\u0080\u0098", "‘")
                .replace("\u00E2\u0080\u0099", "’")
                .replace("\u00C2", "");
    }
}
