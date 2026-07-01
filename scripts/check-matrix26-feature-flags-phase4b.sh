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

require_file "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleVisibilityMapper.java"
require_file "src/main/resources/templates/admin/system_modules/visibility.html"
require_file "docs/project/matrix26-feature-flags-phase4b-test-guide.md"

require_grep "systemModuleFlags" "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleVisibilityMapper.java"
require_grep "featureProperties" "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleVisibilityMapper.java"
require_grep "restaurant_cash" "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleService.java"
require_grep "moduleRestaurantCashEnabled" "src/main/java/com/ecoamazonas/eco_agua/config/GlobalModelAttributes.java"
require_grep "moduleRestaurantQrEnabled" "src/main/java/com/ecoamazonas/eco_agua/config/GlobalModelAttributes.java"
require_grep "SystemModuleVisibilityMapper.featureProperties" "src/main/java/com/ecoamazonas/eco_agua/platform/PlatformRuntimeService.java"
require_grep "SystemModuleVisibilityMapper.systemModuleFlags" "src/main/java/com/ecoamazonas/eco_agua/platform/control/Matrix26TargetDatabaseService.java"
require_grep "runtimeFeatureProperties" "src/main/java/com/ecoamazonas/eco_agua/platform/control/Matrix26TargetDatabaseService.java"
require_grep "GetMapping\(\"/visibility\"\)" "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleAdminController.java"
require_grep "admin/system_modules/visibility" "src/main/java/com/ecoamazonas/eco_agua/config/SystemModuleAdminController.java"
require_grep "moduleRestaurantCashEnabled" "src/main/resources/templates/fragments/sidebar.html"
require_grep "moduleRestaurantQrEnabled" "src/main/resources/templates/fragments/sidebar.html"
require_grep "system_module_visibility" "src/main/resources/templates/fragments/sidebar.html"

if grep -RniE "DROP DATABASE|DROP TABLE|TRUNCATE|Files\.delete|deleteRecursively|archive-destruction-execution-enabled=true" \
  src/main/java/com/ecoamazonas/eco_agua/config \
  src/main/resources/templates/admin/system_modules \
  >/tmp/matrix26-phase4b-dangerous.txt 2>/dev/null; then
  cat /tmp/matrix26-phase4b-dangerous.txt >&2
  fail "Phase 4B must not introduce destructive operations."
fi

echo "Matrix26 Feature Flags Phase 4B static checks passed."
