#!/usr/bin/env bash
set -euo pipefail

required_files=(
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/security/Matrix26ControlPermission.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/security/Matrix26ControlRole.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/security/Matrix26ControlSecurityInitializer.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/security/Matrix26ControlSecurityService.java"
  "src/main/java/com/ecoamazonas/eco_agua/platform/control/security/Matrix26ControlSecurityController.java"
  "src/main/resources/templates/control_center/security/index.html"
)

for file in "${required_files[@]}"; do
  if [ ! -f "$file" ]; then
    echo "Missing required file: $file" >&2
    exit 1
  fi
done

grep -R "MATRIX26_VIEWER" src/main/java/com/ecoamazonas/eco_agua/platform/control/security >/dev/null
grep -R "MATRIX26_PURGE_MANAGER" src/main/java/com/ecoamazonas/eco_agua/platform/control/security >/dev/null
grep -R "matrix26.purge.manage" src/main/java/com/ecoamazonas/eco_agua/platform/control/security >/dev/null
grep -R "matrix26.security.admin" src/main/java/com/ecoamazonas/eco_agua/platform/control/security >/dev/null
grep -R "control-center/security" src/main/java src/main/resources/templates >/dev/null
grep -R "MATRIX26_RUNTIME_ACCESS" src/main/java/com/ecoamazonas/eco_agua/security/SecurityConfig.java >/dev/null
grep -R "MATRIX26_PURGE_ACCESS" src/main/java/com/ecoamazonas/eco_agua/security/SecurityConfig.java >/dev/null

if grep -RniE "DROP DATABASE|DROP SCHEMA|Files\.delete|deleteRecursively|deleteIfExists|package\.m26backup|database\.sql\.gz|instance-files\.zip" \
  src/main/java/com/ecoamazonas/eco_agua/platform/control/security \
  src/main/resources/templates/control_center/security; then
  echo "Unexpected destructive operation found in Phase 3I.3 security files." >&2
  exit 1
fi

echo "Matrix26 Operations Phase 3I.3 static checks passed."
