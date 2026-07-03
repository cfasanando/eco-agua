# GastoClaro Personal - 5C.1A Checkbox Persistence Hotfix Report

## Cambios

- Removed manual hidden false fields for checkbox-backed booleans.
- Keeps Thymeleaf `th:field` binding for boolean fields.
- Covers fixed expenses, income sources and debts.

## Archivos

- `src/main/resources/templates/personal_finance/fixed_expenses.html`
- `src/main/resources/templates/personal_finance/income_sources.html`
- `src/main/resources/templates/personal_finance/debts.html`
- `scripts/check-gastoclaro-personal-phase5c1a-checkbox-hotfix.sh`
- `docs/project/gastoclaro-personal-phase5c1a-checkbox-hotfix-test-guide.md`

## Riesgo

Bajo. Solo cambia el HTML de formularios; no modifica tablas, servicios, controladores, generación mensual ni cronogramas.
