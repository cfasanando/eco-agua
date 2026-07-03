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

require_file src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceDebtScheduleMode.java
require_file src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceScheduleLineType.java
require_file src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceDebtScheduleLine.java
require_file src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceDebtScheduleLineRepository.java
require_file src/main/resources/templates/personal_finance/debt_schedule.html

require_grep 'personal_finance_debt_schedule_line' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceModuleInitializer.java
require_grep 'schedule_mode' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceDebt.java
require_grep 'generateDebtSchedule' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceService.java
require_grep 'generateMonthlyObligations' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceService.java
require_grep 'PostMapping\("/monthly-plan/generate-obligations"\)' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceController.java
require_grep 'GetMapping\("/debts/\{id\}/schedule"\)' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceController.java
require_grep 'Cronograma de deuda' src/main/resources/templates/personal_finance/debt_schedule.html
require_grep 'Generar obligaciones del mes' src/main/resources/templates/personal_finance/monthly_plan.html
require_grep 'Modo de cronograma' src/main/resources/templates/personal_finance/debts.html

if grep -RniE 'DROP[[:space:]]+DATABASE|DROP[[:space:]]+SCHEMA|TRUNCATE[[:space:]]+TABLE|DELETE[[:space:]]+FROM[[:space:]]+personal_finance|Files\.delete|deleteRecursively|PURGE INSTANCE|DESTROY ARCHIVE' \
  src/main/java/com/ecoamazonas/eco_agua/personalfinance \
  src/main/resources/templates/personal_finance >/tmp/gastoclaro_phase5c_destructive.txt 2>&1; then
  cat /tmp/gastoclaro_phase5c_destructive.txt >&2
  fail "Unexpected destructive pattern found."
fi

echo "GastoClaro Personal Phase 5C static checks passed."
