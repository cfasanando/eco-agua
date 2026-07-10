package com.ecoamazonas.eco_agua.personalfinance;

public record PersonalFinanceDebtScheduleGenerationResult(
        int created,
        int updated,
        int removed,
        int protectedLines,
        int unchanged,
        boolean lenderAmortization
) {
    public int affected() {
        return created + updated + removed;
    }

    public String summary() {
        StringBuilder message = new StringBuilder();
        if (lenderAmortization) {
            message.append("Cronograma de prestamista calculado con capital fijo e interés sobre saldo pendiente.");
        } else {
            message.append("Cronograma generado.");
        }
        message.append(" Nuevas: ").append(created)
                .append(", actualizadas: ").append(updated);
        if (removed > 0) {
            message.append(", retiradas: ").append(removed);
        }
        if (protectedLines > 0) {
            message.append(", protegidas por tener pagos: ").append(protectedLines);
        }
        if (unchanged > 0) {
            message.append(", sin cambios: ").append(unchanged);
        }
        return message.toString();
    }

    public static PersonalFinanceDebtScheduleGenerationResult empty(boolean lenderAmortization) {
        return new PersonalFinanceDebtScheduleGenerationResult(0, 0, 0, 0, 0, lenderAmortization);
    }
}
