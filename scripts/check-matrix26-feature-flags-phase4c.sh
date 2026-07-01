#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

require_file() {
  [[ -f "$1" ]] || fail "Missing file: $1"
}

require_grep() {
  local pattern="$1"
  local file="$2"
  grep -qE "$pattern" "$file" || fail "Pattern not found in $file: $pattern"
}

require_file "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleAccessFilter.java"
require_file "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleRouteAccessService.java"
require_file "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleRouteRule.java"
require_file "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleRouteAccessDecision.java"
require_file "src/main/resources/templates/admin/system_modules/visibility.html"
require_file "src/main/resources/templates/error.html"
require_file "docs/project/matrix26-feature-flags-phase4c-test-guide.md"

require_grep "SC_FORBIDDEN" "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleAccessFilter.java"
require_grep "SystemModuleRouteAccessService" "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleAccessFilter.java"
require_grep "restaurant_cash" "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleRouteAccessService.java"
require_grep "restaurant_qr" "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleRouteAccessService.java"
require_grep "restaurant_recipes" "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleRouteAccessService.java"
require_grep "restaurant_reservations" "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleRouteAccessService.java"
require_grep "routeDiagnostics" "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleAdminController.java"
require_grep "routeRuleCount" "src/main/resources/templates/admin/system_modules/visibility.html"
require_grep "Bloqueada 403" "src/main/resources/templates/admin/system_modules/visibility.html"
require_grep "systemModuleAccessDenied" "src/main/resources/templates/error.html"

if grep -RniE "DROP DATABASE|DROP TABLE|TRUNCATE|Files\.delete|deleteRecursively|archive-destruction-execution-enabled=true" \
  src/main/java/com/ecoamazonas/eco_agua/config \
  src/main/resources/templates/admin/system_modules \
  src/main/resources/templates/error.html \
  >/tmp/matrix26-phase4c-dangerous.txt 2>/dev/null; then
  cat /tmp/matrix26-phase4c-dangerous.txt >&2
  fail "Phase 4C must not introduce destructive operations."
fi

if grep -RniE "@PostMapping|PostMapping\(" \
  src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleRouteAccessService.java \
  src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleAccessFilter.java \
  >/tmp/matrix26-phase4c-post.txt 2>/dev/null; then
  cat /tmp/matrix26-phase4c-post.txt >&2
  fail "Phase 4C route protection must not add new POST endpoints."
fi

echo "Matrix26 Feature Flags Phase 4C static checks passed."
