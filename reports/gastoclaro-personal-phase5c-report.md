# GastoClaro Personal - Fase 5C - Reporte

## Alcance implementado

Se agregó soporte inicial para cronogramas y obligaciones generadas desde deuda.

## Archivos principales

- `PersonalFinanceDebtScheduleMode.java`
- `PersonalFinanceScheduleLineType.java`
- `PersonalFinanceDebtScheduleLine.java`
- `PersonalFinanceDebtScheduleLineRepository.java`
- `PersonalFinanceService.java`
- `PersonalFinanceController.java`
- `PersonalFinanceModuleInitializer.java`
- `personal_finance/debt_schedule.html`
- `personal_finance/debts.html`
- `personal_finance/monthly_plan.html`

## Tablas nuevas y columnas

Nueva tabla:

- `personal_finance_debt_schedule_line`

Nuevas columnas en deuda:

- `schedule_mode`
- `schedule_start_date`
- `schedule_end_date`
- `installment_count`
- `auto_generate_monthly`

Nueva columna en obligación:

- `schedule_line_id`

## Funcionalidad

- Registro de modo de cronograma por deuda.
- Generación de cronograma rápido.
- Fila manual de cronograma.
- Generación de obligaciones del mes desde filas de cronograma.
- Evita duplicar obligaciones ya generadas.
- Mantiene datos separados por usuario.

## Seguridad y privacidad

No se incluyen datos personales reales en código, seeds ni SQL versionado. La información financiera real debe cargarse desde la interfaz y permanecer en la base local del usuario.

## Verificación

El script `scripts/check-gastoclaro-personal-phase5c.sh` valida archivos, rutas y patrones destructivos.
