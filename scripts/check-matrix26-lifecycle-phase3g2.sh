#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA_DIR="$PROJECT_ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/lifecycle/decommission"
TEMPLATE_DIR="$PROJECT_ROOT/src/main/resources/templates/control_center/lifecycle/decommission"
RUNTIME_CONTROL="$PROJECT_ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/operations/Matrix26RuntimeControlService.java"
SIDEBAR="$PROJECT_ROOT/src/main/resources/templates/control_center/fragments/sidebar.html"
CONFIG_SCRIPT="$PROJECT_ROOT/scripts/configure-matrix26-lifecycle-phase3g2.sh"

required_java=(
  Matrix26DecommissionController.java
  Matrix26DecommissionInitializer.java
  Matrix26DecommissionRepository.java
  Matrix26DecommissionService.java
  Matrix26DecommissionProperties.java
  Matrix26DecommissionJob.java
  Matrix26DecommissionStatus.java
)
for file in "${required_java[@]}"; do
  test -f "$JAVA_DIR/$file"
done
echo "Persistent decommission jobs, checks, schedules, and events: OK"

grep -q "createManualFullBackup" "$JAVA_DIR/Matrix26DecommissionService.java"
grep -q "Matrix26BackupRetentionClass.FINAL" "$JAVA_DIR/Matrix26DecommissionService.java"
grep -q "verifyEncryptedBackup" "$JAVA_DIR/Matrix26DecommissionService.java"
grep -q "protectedFlag" "$JAVA_DIR/Matrix26DecommissionService.java"
echo "Mandatory encrypted FINAL archive and independent verification: OK"

grep -q 'Only a SUSPENDED laboratory can be decommissioned' "$JAVA_DIR/Matrix26DecommissionService.java"
grep -q 'matrix26-appearance-lab' "$JAVA_DIR/Matrix26DecommissionProperties.java"
grep -q 'Protected instances are read only' "$JAVA_DIR/Matrix26DecommissionService.java"
echo "Suspended laboratory allowlist and protected-instance boundary: OK"

if grep -RniE 'DROP[[:space:]]+(DATABASE|SCHEMA)|Files\.delete|deleteIfExists|deleteRecursively|removeDirectory|DELETE[[:space:]]+FROM[[:space:]]+platform_business_client' "$JAVA_DIR"; then
  echo "Unexpected destructive operation detected in Phase 3G.2." >&2
  exit 1
fi
echo "No database, runtime, resource, module, or backup deletion: OK"

grep -q 'disableSchedules' "$JAVA_DIR/Matrix26DecommissionService.java"
grep -q "status = 'DECOMMISSIONED'" "$JAVA_DIR/Matrix26DecommissionRepository.java"
grep -q 'cannot be controlled as an active runtime' "$RUNTIME_CONTROL"
echo "Schedule shutdown, historical state, and Runtime Control lockout: OK"

grep -Fq '@GetMapping("/{jobId:\\d+}")' "$JAVA_DIR/Matrix26DecommissionController.java"
grep -Fq '@PostMapping("/{jobId:\\d+}/execute")' "$JAVA_DIR/Matrix26DecommissionController.java"
grep -q 'PREPARE DECOMMISSION' "$JAVA_DIR/Matrix26DecommissionService.java"
grep -q 'DECOMMISSION ' "$JAVA_DIR/Matrix26DecommissionService.java"
echo "Exact confirmations and numeric route boundaries: OK"

for file in index.html new.html detail.html decommissioned.html; do
  test -f "$TEMPLATE_DIR/$file"
done
grep -q '/control-center/lifecycle/decommission' "$SIDEBAR"
echo "Decommission templates and navigation: OK"

grep -q 'application.properties.phase3g2.bak' "$CONFIG_SCRIPT"
grep -q 'matrix26.control-center.lifecycle.decommission.enabled=true' "$CONFIG_SCRIPT"
if grep -Ei 'MATRIX26_BACKUP_MASTER_KEY[[:space:]]*=|spring\.datasource\.password[[:space:]]*=' "$CONFIG_SCRIPT"; then
  echo "Configuration script must not write secrets." >&2
  exit 1
fi
echo "Configuration preserves credentials and enables only the laboratory decommission allowlist: OK"

echo "Matrix26 Lifecycle Manager Phase 3G.2 static checks passed."
