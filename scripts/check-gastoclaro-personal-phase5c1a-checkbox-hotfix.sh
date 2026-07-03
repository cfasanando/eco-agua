#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

required_files=(
  "src/main/resources/templates/personal_finance/fixed_expenses.html"
  "src/main/resources/templates/personal_finance/income_sources.html"
  "src/main/resources/templates/personal_finance/debts.html"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "Missing required file: $file" >&2
    exit 1
  fi
done

if grep -RniE 'type="hidden" name="(autoGenerateMonthly|mandatory|active|fixedPayment)" value="false"' \
  src/main/resources/templates/personal_finance; then
  echo "Manual false hidden checkbox fields remain in GastoClaro templates." >&2
  exit 1
fi

grep -q 'th:field="\*{autoGenerateMonthly}"' src/main/resources/templates/personal_finance/fixed_expenses.html
grep -q 'th:field="\*{mandatory}"' src/main/resources/templates/personal_finance/fixed_expenses.html
grep -q 'th:field="\*{active}"' src/main/resources/templates/personal_finance/fixed_expenses.html
grep -q 'th:field="\*{autoGenerateMonthly}"' src/main/resources/templates/personal_finance/income_sources.html
grep -q 'th:field="\*{active}"' src/main/resources/templates/personal_finance/income_sources.html
grep -q 'th:field="\*{fixedPayment}"' src/main/resources/templates/personal_finance/debts.html
grep -q 'th:field="\*{autoGenerateMonthly}"' src/main/resources/templates/personal_finance/debts.html

echo "GastoClaro checkbox persistence hotfix static checks passed."
