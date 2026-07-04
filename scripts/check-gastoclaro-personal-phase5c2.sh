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

BASE=src/main/java/com/ecoamazonas/eco_agua/personalfinance
TEMPLATES=src/main/resources/templates/personal_finance

require_file "$BASE/PersonalFinanceCollectionStatus.java"
require_file "$BASE/PersonalFinanceNegotiationStatus.java"
require_file "$BASE/PersonalFinanceDelinquentDebtItem.java"
require_file "$BASE/PersonalFinanceVoluntaryPaymentForm.java"
require_file "$TEMPLATES/debt_voluntary_payment.html"
require_file "$TEMPLATES/debts.html"
require_file "$TEMPLATES/monthly_plan.html"

require_grep 'previous_monthly_payment' "$BASE/PersonalFinanceModuleInitializer.java"
require_grep 'delinquency_start_date' "$BASE/PersonalFinanceModuleInitializer.java"
require_grep 'collection_status' "$BASE/PersonalFinanceModuleInitializer.java"
require_grep 'negotiation_status' "$BASE/PersonalFinanceModuleInitializer.java"
require_grep 'next_review_date' "$BASE/PersonalFinanceModuleInitializer.java"
require_grep 'TRACKING_ONLY' "$BASE/PersonalFinanceService.java"
require_grep 'cancelGeneratedObligationsForTrackingDebt' "$BASE/PersonalFinanceService.java"
require_grep 'createVoluntaryPayment' "$BASE/PersonalFinanceService.java"
require_grep 'DEBT_VOLUNTARY_PAYMENT' "$BASE/PersonalFinanceObligationSourceType.java"
require_grep 'Deudas en mora / seguimiento' "$TEMPLATES/monthly_plan.html"
require_grep 'No se suman al pago exigible' "$TEMPLATES/monthly_plan.html"
require_grep 'Seguimiento de mora y negociación' "$TEMPLATES/debts.html"
require_grep 'Crear abono voluntario' "$TEMPLATES/debt_voluntary_payment.html"

if grep -RniE 'DROP[[:space:]]+DATABASE|DROP[[:space:]]+SCHEMA|TRUNCATE[[:space:]]+TABLE|DELETE[[:space:]]+FROM[[:space:]]+personal_finance|Files\.delete|deleteRecursively|PURGE INSTANCE|DESTROY ARCHIVE' \
  "$BASE" "$TEMPLATES" >/tmp/gastoclaro_phase5c2_destructive.txt 2>&1; then
  cat /tmp/gastoclaro_phase5c2_destructive.txt >&2
  fail "Unexpected destructive pattern found."
fi

if grep -RniE 'Nicolas|Interbank|BCP|Casanova|Juanita|Monica|Jeanpierr|Ursula|Doctora' \
  "$BASE" "$TEMPLATES" >/tmp/gastoclaro_phase5c2_personal_data.txt 2>&1; then
  cat /tmp/gastoclaro_phase5c2_personal_data.txt >&2
  fail "Personal debt data must not be committed in source files."
fi

echo "GastoClaro Personal Phase 5C.2 static checks passed."
