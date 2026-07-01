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

require_file "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance/Matrix26FeatureFlagAcceptanceController.java"
require_file "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance/Matrix26FeatureFlagAcceptanceService.java"
require_file "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance/Matrix26FeatureFlagAcceptanceMatrix.java"
require_file "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance/Matrix26FeatureFlagAcceptanceGroup.java"
require_file "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance/Matrix26FeatureFlagAcceptanceItem.java"
require_file "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance/Matrix26FeatureFlagAcceptanceMetric.java"
require_file "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance/Matrix26FeatureFlagAcceptanceRisk.java"
require_file "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance/Matrix26FeatureFlagAcceptanceStatus.java"
require_file "src/main/resources/templates/control_center/modules/acceptance/index.html"
require_file "docs/project/matrix26-feature-flags-phase4d-test-guide.md"

require_grep "control-center/modules/acceptance" "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance/Matrix26FeatureFlagAcceptanceController.java"
require_grep "Matrix26ModuleActivationService" "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance/Matrix26FeatureFlagAcceptanceService.java"
require_grep "SystemModuleRouteAccessService" "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance/Matrix26FeatureFlagAcceptanceService.java"
require_grep "SystemModuleService" "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance/Matrix26FeatureFlagAcceptanceService.java"
require_grep "4A Module Activation Center" "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance/Matrix26FeatureFlagAcceptanceService.java"
require_grep "4B runtime flags" "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance/Matrix26FeatureFlagAcceptanceService.java"
require_grep "4C route guard" "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance/Matrix26FeatureFlagAcceptanceService.java"
require_grep "matrix26_module_acceptance" "src/main/resources/templates/control_center/fragments/sidebar.html"
require_grep "Feature Flags acceptance" "src/main/resources/templates/control_center/modules/acceptance/index.html"

if grep -RniE "@PostMapping|PostMapping\(" \
  src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance \
  >/tmp/matrix26-phase4d-post.txt 2>/dev/null; then
  cat /tmp/matrix26-phase4d-post.txt >&2
  fail "Phase 4D acceptance must remain read-only and must not add POST endpoints."
fi

if grep -RniE "DROP DATABASE|DROP TABLE|TRUNCATE|Files\.delete|deleteRecursively|archive-destruction-execution-enabled=true|PURGE INSTANCE|DESTROY ARCHIVE" \
  src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/acceptance \
  src/main/resources/templates/control_center/modules/acceptance \
  >/tmp/matrix26-phase4d-dangerous.txt 2>/dev/null; then
  cat /tmp/matrix26-phase4d-dangerous.txt >&2
  fail "Phase 4D must not introduce destructive operations."
fi

echo "Matrix26 Feature Flags Phase 4D static checks passed."
