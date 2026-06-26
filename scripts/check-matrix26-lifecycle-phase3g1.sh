#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA_DIR="$PROJECT_ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/lifecycle"
TEMPLATE_DIR="$PROJECT_ROOT/src/main/resources/templates/control_center/lifecycle"
SIDEBAR="$PROJECT_ROOT/src/main/resources/templates/control_center/fragments/sidebar.html"
CONFIG_SCRIPT="$PROJECT_ROOT/scripts/configure-matrix26-lifecycle-phase3g1.sh"

required_files=(
  "$JAVA_DIR/Matrix26LifecycleAction.java"
  "$JAVA_DIR/Matrix26LifecycleStatus.java"
  "$JAVA_DIR/Matrix26LifecycleProperties.java"
  "$JAVA_DIR/Matrix26LifecycleInitializer.java"
  "$JAVA_DIR/Matrix26LifecycleRepository.java"
  "$JAVA_DIR/Matrix26LifecycleService.java"
  "$JAVA_DIR/Matrix26LifecycleController.java"
  "$TEMPLATE_DIR/index.html"
  "$TEMPLATE_DIR/detail.html"
  "$CONFIG_SCRIPT"
)
for file in "${required_files[@]}"; do
  [[ -f "$file" ]] || { echo "Missing required file: $file" >&2; exit 1; }
done

grep -q 'matrix26_lifecycle_job' "$JAVA_DIR/Matrix26LifecycleInitializer.java"
grep -q 'matrix26_lifecycle_schedule_state' "$JAVA_DIR/Matrix26LifecycleInitializer.java"
grep -q 'matrix26_lifecycle_event' "$JAVA_DIR/Matrix26LifecycleInitializer.java"
echo "Persistent lifecycle jobs, schedule snapshots, and events: OK"

grep -q 'matrix26-appearance-lab' "$JAVA_DIR/Matrix26LifecycleProperties.java"
grep -q 'Protected instances are read only' "$JAVA_DIR/Matrix26LifecycleService.java"
grep -q 'outside the lifecycle laboratory allowlist' "$JAVA_DIR/Matrix26LifecycleService.java"
echo "Laboratory allowlist and protected-instance boundary: OK"

grep -q "verification_status = 'VERIFIED'" "$JAVA_DIR/Matrix26LifecycleRepository.java"
grep -q 'encrypted = 1' "$JAVA_DIR/Matrix26LifecycleRepository.java"
grep -q 'maximumVerifiedBackupAgeHours' "$JAVA_DIR/Matrix26LifecycleProperties.java"
echo "Recent encrypted and verified backup gate: OK"

grep -q 'snapshotEnabledSchedules' "$JAVA_DIR/Matrix26LifecycleService.java"
grep -q 'pauseSnapshottedSchedules' "$JAVA_DIR/Matrix26LifecycleService.java"
grep -q 'restoreSnapshottedSchedules' "$JAVA_DIR/Matrix26LifecycleService.java"
grep -q 'SCHEDULE_COMPENSATION' "$JAVA_DIR/Matrix26LifecycleService.java"
echo "Backup schedule pause, restoration, and failure compensation: OK"

grep -q 'runtimeControlService.stop' "$JAVA_DIR/Matrix26LifecycleService.java"
grep -q 'runtimeControlService.start' "$JAVA_DIR/Matrix26LifecycleService.java"
grep -q 'The reactivated runtime did not pass the HTTP health check' "$JAVA_DIR/Matrix26LifecycleService.java"
echo "Runtime Control stop, start, port ownership, and health verification: OK"

for table in matrix26_backup_job matrix26_backup_schedule_execution matrix26_runtime_operation matrix26_restore_job matrix26_inplace_restore_job; do
  grep -q "$table" "$JAVA_DIR/Matrix26LifecycleRepository.java" || {
    echo "Missing active-operation boundary for $table" >&2
    exit 1
  }
done
echo "Backup, runtime, clone restore, and in-place restore conflict gates: OK"

grep -q 'SUSPEND ' "$JAVA_DIR/Matrix26LifecycleService.java"
grep -q 'REACTIVATE ' "$JAVA_DIR/Matrix26LifecycleService.java"
grep -Fq '@PostMapping("/instances/{instanceId:\\d+}/suspend")' "$JAVA_DIR/Matrix26LifecycleController.java"
grep -Fq '@PostMapping("/instances/{instanceId:\\d+}/reactivate")' "$JAVA_DIR/Matrix26LifecycleController.java"
echo "Exact confirmations and numeric route boundaries: OK"

if grep -nE 'spring\.datasource\.password|MATRIX26_BACKUP_MASTER_KEY|password=' "$CONFIG_SCRIPT"; then
  echo "The Phase 3G.1 configuration script must not write credentials." >&2
  exit 1
fi
grep -q 'allowed-instance-codes=matrix26-appearance-lab' "$CONFIG_SCRIPT"
echo "Configuration preserves credentials and enables only the laboratory lifecycle allowlist: OK"

python3 - "$TEMPLATE_DIR/index.html" "$TEMPLATE_DIR/detail.html" <<'PY'
from html.parser import HTMLParser
from pathlib import Path
import sys
class Parser(HTMLParser):
    def __init__(self):
        super().__init__(); self.forms=0; self.closed=0
    def handle_starttag(self, tag, attrs):
        if tag == 'form': self.forms += 1
    def handle_endtag(self, tag):
        if tag == 'form': self.closed += 1
for name in sys.argv[1:]:
    parser=Parser(); parser.feed(Path(name).read_text(encoding='utf-8'))
    if parser.forms != parser.closed:
        raise SystemExit(f'Unbalanced form tags in {name}: {parser.forms}/{parser.closed}')
print('Lifecycle templates and confirmation forms: OK')
PY

grep -q '/control-center/lifecycle' "$SIDEBAR"
echo "Lifecycle Manager navigation: OK"

echo "Matrix26 Lifecycle Manager Phase 3G.1 static checks passed."
