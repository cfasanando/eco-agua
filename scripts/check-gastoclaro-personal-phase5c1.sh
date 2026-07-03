#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

require_file() {
  [ -f "$1" ] || fail "Missing file: $1"
}

require_grep() {
  local pattern="$1"
  local file="$2"
  grep -qE "$pattern" "$file" || fail "Pattern not found in $file: $pattern"
}

require_file src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceMonthGenerationResult.java
require_file src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceIncomeSource.java
require_file src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceFixedExpense.java
require_file src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceService.java
require_file src/main/resources/templates/personal_finance/income_sources.html
require_file src/main/resources/templates/personal_finance/fixed_expenses.html
require_file src/main/resources/templates/personal_finance/monthly_plan.html

require_grep 'auto_generate_monthly' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceModuleInitializer.java
require_grep 'expected_day' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceModuleInitializer.java
require_grep 'start_date' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceModuleInitializer.java
require_grep 'end_date' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceModuleInitializer.java
require_grep 'generateMonthlyPlan' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceService.java
require_grep 'generateIncomeEventsForMonth' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceService.java
require_grep 'generateFixedExpenseObligationsForMonth' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceService.java
require_grep 'ensureDebtScheduleLinesForMonth' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceService.java
require_grep 'existsByUserAndIncomeSourceAndExpectedDateBetween' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceIncomeEventRepository.java
require_grep 'existsByUserAndSourceTypeAndSourceIdAndDueDateBetween' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinancePaymentObligationRepository.java
require_grep 'Generar / actualizar mes' src/main/resources/templates/personal_finance/monthly_plan.html
require_grep 'Nueva fuente recurrente' src/main/resources/templates/personal_finance/income_sources.html
require_grep 'Generar cada mes' src/main/resources/templates/personal_finance/fixed_expenses.html

if grep -RniE 'DROP[[:space:]]+DATABASE|DROP[[:space:]]+SCHEMA|TRUNCATE[[:space:]]+TABLE|DELETE[[:space:]]+FROM[[:space:]]+personal_finance|Files\.delete|deleteRecursively|PURGE INSTANCE|DESTROY ARCHIVE' \
  src/main/java/com/ecoamazonas/eco_agua/personalfinance \
  src/main/resources/templates/personal_finance >/tmp/gastoclaro_phase5c1_destructive.txt 2>&1; then
  cat /tmp/gastoclaro_phase5c1_destructive.txt >&2
  fail "Unexpected destructive pattern found."
fi

echo "GastoClaro Personal Phase 5C.1 static checks passed."
