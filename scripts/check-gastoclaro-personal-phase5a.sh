#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

required_files=(
  "src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceController.java"
  "src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceService.java"
  "src/main/java/com/ecoamazonas/eco_agua/personalfinance/PersonalFinanceModuleInitializer.java"
  "src/main/resources/templates/personal_finance/dashboard.html"
  "src/main/resources/templates/personal_finance/debts.html"
  "src/main/resources/templates/personal_finance/fixed_expenses.html"
  "src/main/resources/templates/personal_finance/income_sources.html"
  "src/main/resources/templates/personal_finance/income_events.html"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "Missing required file: $file" >&2
    exit 1
  fi
done

grep -R "personal_finance" -n src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleService.java >/dev/null
grep -R "modulePersonalFinanceEnabled" -n src/main/java/com/ecoamazonas/eco_agua/config/GlobalModelAttributes.java >/dev/null
grep -R "/gasto-claro" -n src/main/resources/templates/fragments/sidebar.html >/dev/null
grep -R "rule(\"/gasto-claro\"" -n src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleRouteAccessService.java >/dev/null

grep -R "DROP DATABASE\|DROP SCHEMA\|TRUNCATE\|DELETE FROM user\|DELETE FROM platform_setting" -n \
  src/main/java/com/ecoamazonas/eco_agua/personalfinance \
  src/main/resources/templates/personal_finance \
  2>/dev/null && {
    echo "Unsafe destructive statement found in GastoClaro Phase 5A files." >&2
    exit 1
  }

grep -R "findByIdAndUser\|currentUser" -n src/main/java/com/ecoamazonas/eco_agua/personalfinance >/dev/null

echo "GastoClaro Personal Phase 5A static checks passed."
