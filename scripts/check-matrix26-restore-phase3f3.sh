#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RESTORE_JAVA="$PROJECT_ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/restores"
TEMPLATE="$PROJECT_ROOT/src/main/resources/templates/control_center/restores/detail.html"
INITIALIZER="$RESTORE_JAVA/Matrix26RestoreInitializer.java"
SERVICE="$RESTORE_JAVA/Matrix26RestoreCleanupService.java"
REPOSITORY="$RESTORE_JAVA/Matrix26RestoreCleanupRepository.java"
CONTROLLER="$RESTORE_JAVA/Matrix26RestoreController.java"
CONFIG_SCRIPT="$PROJECT_ROOT/scripts/configure-matrix26-restore-phase3f3.sh"

required_files=(
  "$RESTORE_JAVA/Matrix26RestoreCleanupStatus.java"
  "$RESTORE_JAVA/Matrix26RestoreCleanupItemStatus.java"
  "$RESTORE_JAVA/Matrix26RestoreCleanupPlan.java"
  "$RESTORE_JAVA/Matrix26RestoreCleanupPlanItem.java"
  "$RESTORE_JAVA/Matrix26RestoreCleanupSnapshot.java"
  "$REPOSITORY"
  "$SERVICE"
  "$TEMPLATE"
  "$CONFIG_SCRIPT"
)
for file in "${required_files[@]}"; do
  [[ -f "$file" ]] || { echo "Missing required file: $file" >&2; exit 1; }
done

grep -q 'matrix26_restore_cleanup_plan' "$INITIALIZER"
grep -q 'matrix26_restore_cleanup_item' "$INITIALIZER"
grep -q 'matrix26_restore_cleanup_event' "$INITIALIZER"
grep -q 'plan_signature VARCHAR(64)' "$INITIALIZER"
echo "Persistent signed cleanup plans, items, and events: OK"

grep -q 'Mac.getInstance("HmacSHA256")' "$SERVICE"
grep -q 'MATRIX26_BACKUP_MASTER_KEY' "$SERVICE"
grep -q 'verifyPlanSignature' "$SERVICE"
grep -q 'snapshotFingerprint' "$SERVICE"
echo "HMAC-SHA256 plan signing and immutable snapshot validation: OK"

for confirmation in \
  'PREPARE CLEANUP ' \
  'STOP RUNTIME ' \
  'REMOVE FILES ' \
  'DROP DATABASE ' \
  'REMOVE REGISTRATION ' \
  'EXECUTE CLEANUP '
do
  grep -q "$confirmation" "$SERVICE" || { echo "Missing confirmation: $confirmation" >&2; exit 1; }
done
echo "Independent destructive confirmations and final execution gate: OK"

grep -q 'matrix26-appearance-lab' "$SERVICE"
grep -q 'matrix26_platform_control' "$SERVICE"
grep -q 'sourceDatabaseName().equalsIgnoreCase' "$SERVICE"
grep -q 'sourceBackupAvailable' "$SERVICE"
grep -q 'SOURCE_BACKUP' "$SERVICE"
grep -q 'Source backup preserved' "$SERVICE"
echo "Source instance, control database, and encrypted backup protections: OK"

grep -q 'containsSymbolicLink' "$SERVICE"
grep -q 'ensureInside' "$SERVICE"
grep -q '.matrix26-restore-reference' "$SERVICE"
grep -q 'RESTORED_CLONE' "$SERVICE"
grep -q 'Port .*unexpected process' "$SERVICE"
echo "Ownership markers, path boundaries, symlink rejection, and port ownership: OK"

python3 - "$SERVICE" <<'PY'
from pathlib import Path
import sys
text = Path(sys.argv[1]).read_text(encoding="utf-8")
order = [
    'case "RUNTIME_PROCESS"',
    'case "MODULE_ASSIGNMENTS"',
    'case "INSTANCE_REGISTRATION"',
    'case "RUNTIME_DIRECTORY"',
    'case "RUNTIME_DATA"',
    'case "DATABASE"',
    'case "TEMPORARY_EXTRACTION"',
]
positions = [text.index(token) for token in order]
if positions != sorted(positions):
    raise SystemExit("Cleanup switch order changed unexpectedly.")
print("Safe cleanup order with database removal after registration and files: OK")
PY

grep -q 'PARTIALLY_CLEANED' "$SERVICE"
grep -q 'item.status().finished()' "$SERVICE"
grep -q 'first unfinished item' "$SERVICE"
grep -q 'RESIDUAL_CHECK_FAILED' "$SERVICE"
grep -q 'public synchronized Matrix26RestoreCleanupPlan execute' "$SERVICE"
grep -q "Interrupted while Matrix26 was offline" "$INITIALIZER"
grep -q "WHERE status = 'CLEANING'" "$INITIALIZER"
echo "Interrupted cleanup resumption, startup recovery, serialization, and residual verification: OK"

grep -q '@PostMapping("/{id}/cleanup/prepare")' "$CONTROLLER"
grep -q '@PostMapping("/{id}/cleanup/{planId}/approve")' "$CONTROLLER"
grep -q '@PostMapping("/{id}/cleanup/{planId}/execute")' "$CONTROLLER"
grep -q 'Controlled incomplete-restore cleanup' "$TEMPLATE"
grep -q 'Independent approvals' "$TEMPLATE"
echo "Cleanup preview, approval, execution routes, and UI: OK"

if grep -nE 'spring\.datasource\.password|MATRIX26_BACKUP_MASTER_KEY|password=' "$CONFIG_SCRIPT"; then
  echo "The Phase 3F.3 configuration script must not write credentials." >&2
  exit 1
fi
grep -q 'cleanup-enabled=true' "$CONFIG_SCRIPT"
echo "Configuration enables cleanup without changing credentials: OK"

python3 - "$TEMPLATE" <<'PY'
from html.parser import HTMLParser
from pathlib import Path
import sys
class Parser(HTMLParser):
    def __init__(self):
        super().__init__(); self.forms=0; self.closed=0
    def handle_starttag(self, tag, attrs):
        if tag == "form": self.forms += 1
    def handle_endtag(self, tag):
        if tag == "form": self.closed += 1
p=Parser(); p.feed(Path(sys.argv[1]).read_text(encoding="utf-8"))
if p.forms != p.closed:
    raise SystemExit(f"Unbalanced form tags: {p.forms}/{p.closed}")
print("Cleanup template forms: OK")
PY

echo "Matrix26 Restore Manager Phase 3F.3 static checks passed."
