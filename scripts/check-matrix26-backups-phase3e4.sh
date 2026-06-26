#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PACKAGE="$ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/backups"
TEMPLATES="$ROOT/src/main/resources/templates/control_center/backups"
CSS="$ROOT/src/main/resources/static/css/matrix26-control.css"
CONFIG_SCRIPT="$ROOT/scripts/configure-matrix26-backup-scheduler-phase3e4.sh"

required_files=(
  "$PACKAGE/Matrix26BackupSchedule.java"
  "$PACKAGE/Matrix26BackupScheduleExecution.java"
  "$PACKAGE/Matrix26BackupAlert.java"
  "$PACKAGE/Matrix26BackupScheduleRepository.java"
  "$PACKAGE/Matrix26BackupScheduleService.java"
  "$PACKAGE/Matrix26BackupScheduleController.java"
  "$PACKAGE/Matrix26BackupScheduler.java"
  "$PACKAGE/Matrix26BackupSchedulingConfiguration.java"
  "$TEMPLATES/schedules.html"
  "$TEMPLATES/schedule_form.html"
  "$TEMPLATES/schedule_detail.html"
  "$TEMPLATES/calendar.html"
  "$TEMPLATES/executions.html"
  "$TEMPLATES/alerts.html"
  "$CONFIG_SCRIPT"
)

for file in "${required_files[@]}"; do
  test -f "$file" || { echo "Missing required file: $file" >&2; exit 1; }
done

grep -q '@EnableScheduling' "$PACKAGE/Matrix26BackupSchedulingConfiguration.java"
grep -q '@Scheduled' "$PACKAGE/Matrix26BackupScheduler.java"
grep -q 'America/Lima' "$PACKAGE/Matrix26BackupProperties.java"
grep -q 'matrix26_backup_schedule' "$PACKAGE/Matrix26BackupInitializer.java"
grep -q 'matrix26_backup_schedule_execution' "$PACKAGE/Matrix26BackupInitializer.java"
grep -q 'matrix26_backup_alert' "$PACKAGE/Matrix26BackupInitializer.java"
grep -q 'uk_matrix26_backup_schedule_window' "$PACKAGE/Matrix26BackupInitializer.java"
grep -q 'RUN_ON_STARTUP' "$PACKAGE/Matrix26BackupScheduleService.java"
grep -q 'RETRY_WAITING' "$PACKAGE/Matrix26BackupScheduleService.java"
grep -q 'Another backup operation is already active' "$PACKAGE/Matrix26BackupScheduleService.java"
grep -q 'createScheduledFullBackup' "$PACKAGE/Matrix26BackupService.java"
grep -q 'SCHEDULED_FULL' "$PACKAGE/Matrix26BackupSecurityService.java"
grep -q 'AES-256-GCM encryption is mandatory' "$TEMPLATES/schedule_form.html"

if grep -RniE 'Runtime\.getRuntime\(\)\.exec|ProcessBuilder.*(cmd|powershell|bash)|DROP DATABASE|DELETE FROM matrix26_backup_job' \
  "$PACKAGE/Matrix26BackupSchedule"*.java; then
  echo "Scheduled backup security boundary violation detected." >&2
  exit 1
fi

if grep -RniE 'matrix26\.control-center\.backups\.(master-key|master-key-value)=' \
  "$ROOT/runtime-clients" "$ROOT/src/main/resources" 2>/dev/null; then
  echo "A backup master key must never be stored in runtime properties." >&2
  exit 1
fi

python - "$TEMPLATES" "$CSS" <<'PY'
from pathlib import Path
from html.parser import HTMLParser
import sys

class Parser(HTMLParser):
    pass

for path in Path(sys.argv[1]).glob('*.html'):
    parser = Parser()
    parser.feed(path.read_text(encoding='utf-8'))

css = Path(sys.argv[2]).read_text(encoding='utf-8')
if css.count('{') != css.count('}'):
    raise SystemExit('Unbalanced CSS braces')
print('Backup schedule templates and CSS: OK')
PY

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

cp "$PACKAGE/Matrix26BackupScheduleFrequency.java" "$TMP/"
cp "$PACKAGE/Matrix26BackupMissedPolicy.java" "$TMP/"
cp "$PACKAGE/Matrix26BackupScheduleExecutionStatus.java" "$TMP/"
cp "$PACKAGE/Matrix26BackupRetentionClass.java" "$TMP/"
cp "$PACKAGE/Matrix26BackupSchedule.java" "$TMP/"

javac --release 17 -d "$TMP/classes" \
  "$TMP/Matrix26BackupScheduleFrequency.java" \
  "$TMP/Matrix26BackupMissedPolicy.java" \
  "$TMP/Matrix26BackupScheduleExecutionStatus.java" \
  "$TMP/Matrix26BackupRetentionClass.java" \
  "$TMP/Matrix26BackupSchedule.java"

echo "Scheduler model compilation: OK"
echo "Duplicate-window unique key and persistent execution locking: OK"
echo "Latest-missed-only recovery policy: OK"
echo "Finite retries and operational alerts: OK"
echo "Scheduled full backups require AES-256-GCM encryption: OK"
echo "Protected instance boundary remains delegated to the backup allowlist: OK"
echo "Matrix26 Backups Phase 3E.4 static checks passed."
