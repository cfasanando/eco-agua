# GastoClaro Personal — Fase 5C.1 Report

## Resumen

Se implementó la capa de recurrencias y generación mensual para evitar cargar manualmente ingresos y gastos repetitivos cada mes.

## Cambios principales

- `PersonalFinanceIncomeSource` ahora soporta frecuencia, día esperado, fecha inicio/fin y auto-generación mensual.
- `PersonalFinanceFixedExpense` ahora soporta fecha inicio/fin y auto-generación mensual.
- `PersonalFinanceService` agrega `generateMonthlyPlan`, que crea o actualiza el mes sin duplicar.
- El plan mensual ahora usa obligaciones e ingresos materializados en el mes, no plantillas virtuales mezcladas.
- El botón del plan mensual ahora se llama **Generar / actualizar mes**.
- La generación respeta pagos/obligaciones ya existentes y no sobrescribe datos.

## Generación mensual

El flujo crea:

1. ingresos desde fuentes recurrentes activas;
2. costo de vida desde gastos fijos activos;
3. líneas de cronograma para deudas auto-generables cuando faltan en el mes;
4. obligaciones desde cronogramas;
5. obligaciones simples para deudas mensuales sin cronograma.

## Seguridad

No se incluyen datos personales reales ni seeds con deudas reales.

## Validación

El script `scripts/check-gastoclaro-personal-phase5c1.sh` valida archivos, rutas, patrones clave y ausencia de operaciones destructivas.
