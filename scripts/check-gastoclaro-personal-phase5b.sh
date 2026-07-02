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

require_file src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinancePaymentObligation.java
require_file src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinancePaymentObligationRepository.java
require_file src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceMonthlyPlan.java
require_file src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceMonthlyPlanItem.java
require_file src/main/resources/templates/personal_finance/monthly_plan.html
require_file src/main/resources/templates/fragments/topbar.html
require_file src/main/resources/templates/fragments/sidebar.html

require_grep 'GetMapping\("/monthly-plan"\)' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceController.java
require_grep 'personal_finance_payment_obligation' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceModuleInitializer.java
require_grep 'holder_type' src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceModuleInitializer.java
require_grep 'topbar-personal-menu' src/main/resources/templates/fragments/topbar.html
require_grep 'GastoClaro is now available from the personal topbar menu' src/main/resources/templates/fragments/sidebar.html
require_grep 'Plan mensual' src/main/resources/templates/personal_finance/_nav.html
require_grep 'gasto-month-hero' src/main/resources/static/css/admin.css

if grep -RniE 'DROP[[:space:]]+DATABASE|DROP[[:space:]]+SCHEMA|TRUNCATE[[:space:]]+TABLE|DELETE[[:space:]]+FROM[[:space:]]+personal_finance|Files\.delete|deleteRecursively|PURGE INSTANCE|DESTROY ARCHIVE' \
  src/main/java/com/ecoamazonas/eco_agua/personalfinance \
  src/main/resources/templates/personal_finance \
  src/main/resources/templates/fragments/topbar.html \
  src/main/resources/templates/fragments/sidebar.html \
  src/main/resources/static/css/admin.css >/tmp/gastoclaro_phase5b_destructive.txt 2>&1; then
  cat /tmp/gastoclaro_phase5b_destructive.txt >&2
  fail "Unexpected destructive pattern found."
fi

echo "GastoClaro Personal Phase 5B static checks passed."
