#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RESTORE_JAVA="$PROJECT_ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/restores"
RESTORE_TEMPLATES="$PROJECT_ROOT/src/main/resources/templates/control_center/restores"
CONTROLLER="$RESTORE_JAVA/Matrix26RestoreController.java"
SERVICE="$RESTORE_JAVA/Matrix26RestoreService.java"
VERIFICATION="$RESTORE_JAVA/Matrix26RestoreVerificationService.java"
INITIALIZER="$RESTORE_JAVA/Matrix26RestoreInitializer.java"
CONFIG_SCRIPT="$PROJECT_ROOT/scripts/configure-matrix26-restore-phase3f2.sh"

required_files=(
  "$RESTORE_JAVA/Matrix26RestoreCheckStatus.java"
  "$RESTORE_JAVA/Matrix26RestoreValidationStatus.java"
  "$RESTORE_JAVA/Matrix26RestoreValidationRun.java"
  "$RESTORE_JAVA/Matrix26RestoreValidationItem.java"
  "$RESTORE_JAVA/Matrix26RestoreDumpSnapshot.java"
  "$RESTORE_JAVA/Matrix26RestoreCleanupPreview.java"
  "$RESTORE_JAVA/Matrix26RestoreResumePlan.java"
  "$VERIFICATION"
  "$RESTORE_TEMPLATES/index.html"
  "$RESTORE_TEMPLATES/detail.html"
  "$CONFIG_SCRIPT"
)

for file in "${required_files[@]}"; do
  [[ -f "$file" ]] || { echo "Missing required file: $file" >&2; exit 1; }
done

grep -q 'matrix26_restore_validation_run' "$INITIALIZER"
grep -q 'matrix26_restore_validation_item' "$INITIALIZER"
grep -q 'matrix26_restore_resume_event' "$INITIALIZER"
echo "Verification and resume metadata tables: OK"

grep -q 'database.sql.gz' "$VERIFICATION"
grep -q 'Matrix26RestoreDumpSnapshot.read' "$VERIFICATION"
grep -q 'DATABASE_SCHEMA' "$VERIFICATION"
grep -q 'DATABASE_ROW_COUNTS' "$VERIFICATION"
grep -q 'SELECT COUNT(\*) FROM' "$VERIFICATION"
echo "Encrypted SQL schema and row-count comparison: OK"

grep -q 'modules.json' "$VERIFICATION"
grep -q 'appearance.json' "$VERIFICATION"
grep -q 'instance-files.zip' "$VERIFICATION"
grep -q 'RESOURCE_HASHES' "$VERIFICATION"
grep -q 'RUNTIME_CONFIGURATION' "$VERIFICATION"
grep -q 'HTTP_ROUTES' "$VERIFICATION"
echo "Modules, appearance, resource hashes, runtime, and HTTP verification: OK"

grep -q '@PostMapping("/{id}/verify")' "$CONTROLLER"
grep -q '@PostMapping("/{id}/resume")' "$CONTROLLER"
grep -q 'validations/{runId}/report' "$CONTROLLER"
grep -q 'resumePlan' "$SERVICE"
grep -q 'executeResume' "$SERVICE"
grep -q 'cleanupPreview' "$SERVICE"
echo "Verification report, safe resumption, and cleanup preview routes: OK"

if grep -RniE 'DROP[[:space:]]+DATABASE|Files\.delete\(targetRuntime|Files\.delete\(targetData|deleteRecursively\(target' "$RESTORE_JAVA"; then
  echo "Destructive clone cleanup was detected in Phase 3F.2." >&2
  exit 1
fi
grep -q 'Dry run only' "$RESTORE_TEMPLATES/detail.html"
grep -q 'No deletion' "$RESTORE_TEMPLATES/detail.html"
echo "Cleanup remains dry-run only: OK"

if grep -nE 'spring\.datasource\.password|MATRIX26_BACKUP_MASTER_KEY|password=' "$CONFIG_SCRIPT"; then
  echo "The Phase 3F.2 configuration script must not write credentials." >&2
  exit 1
fi
grep -q 'verification-enabled=true' "$CONFIG_SCRIPT"
grep -q 'resume-enabled=true' "$CONFIG_SCRIPT"
echo "Configuration preserves credentials and enables only Phase 3F.2 flags: OK"

python3 - "$RESTORE_TEMPLATES" <<'PY'
from pathlib import Path
from html.parser import HTMLParser
import sys

class FormParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.forms = 0
        self.closed = 0
    def handle_starttag(self, tag, attrs):
        if tag == "form":
            self.forms += 1
    def handle_endtag(self, tag):
        if tag == "form":
            self.closed += 1

for path in Path(sys.argv[1]).glob("*.html"):
    parser = FormParser()
    parser.feed(path.read_text(encoding="utf-8"))
    if parser.forms != parser.closed:
        raise SystemExit(f"Unbalanced form tags in {path.name}: {parser.forms}/{parser.closed}")
print("Restore templates and confirmation forms: OK")
PY

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
mkdir -p "$TMP_DIR/src/com/ecoamazonas/eco_agua/platform/control/restores"
cp "$RESTORE_JAVA/Matrix26RestoreDumpSnapshot.java" "$TMP_DIR/src/com/ecoamazonas/eco_agua/platform/control/restores/"
cat > "$TMP_DIR/src/com/ecoamazonas/eco_agua/platform/control/restores/DumpSnapshotCheck.java" <<'JAVA'
package com.ecoamazonas.eco_agua.platform.control.restores;

import java.nio.file.Path;

public class DumpSnapshotCheck {
    public static void main(String[] args) throws Exception {
        var snapshot = Matrix26RestoreDumpSnapshot.read(Path.of(args[0]));
        if (!snapshot.createStatements().containsKey("sample_table")) {
            throw new IllegalStateException("CREATE TABLE was not detected.");
        }
        if (snapshot.rowCounts().getOrDefault("sample_table", -1L) != 3L) {
            throw new IllegalStateException("Extended INSERT row count is incorrect: " + snapshot.rowCounts());
        }
        if (snapshot.rowCounts().getOrDefault("empty_table", -1L) != 0L) {
            throw new IllegalStateException("Empty table row count is incorrect.");
        }
    }
}
JAVA
cat > "$TMP_DIR/sample.sql" <<'SQL'
CREATE TABLE `sample_table` (
  `id` bigint NOT NULL,
  `label` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO `sample_table` VALUES (1,'one'),(2,'two, comma'),(3,'parenthesis ) and escaped \' quote');
CREATE TABLE `empty_table` (`id` bigint NOT NULL) ENGINE=InnoDB;
SQL
gzip -c "$TMP_DIR/sample.sql" > "$TMP_DIR/sample.sql.gz"
javac --release 17 -d "$TMP_DIR/classes" $(find "$TMP_DIR/src" -name '*.java')
java -cp "$TMP_DIR/classes" com.ecoamazonas.eco_agua.platform.control.restores.DumpSnapshotCheck "$TMP_DIR/sample.sql.gz"
echo "MariaDB dump snapshot parser and extended INSERT counting: OK"

echo "Matrix26 Restore Manager Phase 3F.2 static checks passed."
