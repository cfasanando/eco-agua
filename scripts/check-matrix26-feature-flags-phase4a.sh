#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

required_files=(
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/Matrix26ModuleActivationController.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/Matrix26ModuleActivationService.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/Matrix26ModuleActivationInitializer.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/Matrix26ModuleActivationEvent.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/modules/Matrix26ModuleActivationEventRepository.java"
  "src/main/resources/templates/control_center/modules/activation.html"
  "src/main/resources/templates/control_center/fragments/sidebar.html"
  "src/main/resources/static/css/matrix26-control.css"
)

for file in "${required_files[@]}"; do
  if [ ! -f "$file" ]; then
    echo "Missing required Phase 4A file: $file" >&2
    exit 1
  fi
done

if ! grep -R "control-center/modules/activation" -n src/main/java/com/ecoamazonas/eco_agua/platform/control/modules src/main/resources/templates/control_center >/dev/null; then
  echo "The module activation route was not found." >&2
  exit 1
fi

if ! grep -R "matrix26_instance_module_activation_event" -n src/main/java/com/ecoamazonas/eco_agua/platform/control/modules >/dev/null; then
  echo "The module activation event table was not found." >&2
  exit 1
fi

if grep -RniE "DROP[[:space:]]+DATABASE|TRUNCATE[[:space:]]+TABLE|Files\.delete|deleteIfExists|deleteRecursively|rm[[:space:]]+-rf|Runtime\.getRuntime\(\)\.exec" \
  src/main/java/com/ecoamazonas/eco_agua/platform/control/modules \
  src/main/resources/templates/control_center/modules >/tmp/matrix26_phase4a_forbidden.txt; then
  echo "Forbidden destructive operation detected in Phase 4A package:" >&2
  cat /tmp/matrix26_phase4a_forbidden.txt >&2
  exit 1
fi

if ! grep -R "matrix26.modules.manage" -n src/main/java/com/ecoamazonas/eco_agua/platform/control/security src/main/java/com/ecoamazonas/eco_agua/security/SecurityConfig.java >/dev/null; then
  echo "Matrix26 module management permission was not found." >&2
  exit 1
fi

echo "Matrix26 Feature Flags Phase 4A static checks passed."
