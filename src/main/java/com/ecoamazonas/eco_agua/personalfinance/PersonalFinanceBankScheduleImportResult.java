package com.ecoamazonas.eco_agua.personalfinance;

import java.util.List;

public record PersonalFinanceBankScheduleImportResult(
        int rowsRead,
        int created,
        int updated,
        int preservedPaid,
        List<String> errors
) {
    public boolean success() {
        return errors == null || errors.isEmpty();
    }

    public String summary() {
        if (!success()) {
            return "Importación detenida. Filas válidas detectadas: " + rowsRead + ".";
        }
        return "Cronograma importado. Filas leídas: " + rowsRead
                + ", creadas: " + created
                + ", actualizadas: " + updated
                + ", pagos preservados: " + preservedPaid + ".";
    }
}
