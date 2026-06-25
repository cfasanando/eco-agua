#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="$ROOT/runtime-clients/matrix26_control/application.properties"
DUMP_TOOL="${1:-C:/wamp64/bin/mariadb/mariadb10.10.2/bin/mariadb-dump.exe}"
BACKUP_ROOT="${2:-C:/Users/PC/Matrix26/backups}"
CONFIG_BACKUP_DIR="$ROOT/runtime-data/matrix26-control/maintenance"
mkdir -p "$CONFIG_BACKUP_DIR"

if [[ ! -f "$TARGET" ]]; then
  echo "Matrix26 runtime configuration was not found: $TARGET" >&2
  exit 1
fi

python - "$TARGET" "$DUMP_TOOL" "$BACKUP_ROOT" "$CONFIG_BACKUP_DIR" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
dump_tool = sys.argv[2].strip()
backup_root = sys.argv[3].strip()
config_backup_dir = Path(sys.argv[4])

if not dump_tool or not backup_root:
    raise SystemExit("Dump tool and backup root must not be empty.")

keys = (
    "matrix26.backup.dump-tool=",
    "matrix26.backup.root=",
    "matrix26.control-center.backups.dump-executable=",
    "matrix26.control-center.backups.root-directory=",
    "matrix26.control-center.backups.runtime-directory=",
    "matrix26.control-center.backups.runtime-data-directory=",
    "matrix26.control-center.backups.maximum-single-file-bytes=",
    "matrix26.control-center.backups.maximum-archive-source-bytes=",
    "matrix26.control-center.backups.diagnostic-log-tail-lines=",
)

original = path.read_text(encoding="utf-8")
lines = [line for line in original.splitlines() if not line.strip().startswith(keys)]
while lines and not lines[-1].strip():
    lines.pop()

lines.extend([
    "",
    "# Matrix26 Backups Phase 3E.2",
    f"matrix26.control-center.backups.dump-executable=${{MATRIX26_MYSQLDUMP_PATH:{dump_tool}}}",
    f"matrix26.control-center.backups.root-directory=${{MATRIX26_BACKUP_ROOT:{backup_root}}}",
    "matrix26.control-center.backups.runtime-directory=runtime-clients",
    "matrix26.control-center.backups.runtime-data-directory=runtime-data",
    "matrix26.control-center.backups.maximum-single-file-bytes=536870912",
    "matrix26.control-center.backups.maximum-archive-source-bytes=2147483648",
    "matrix26.control-center.backups.diagnostic-log-tail-lines=400",
])

config_backup_dir.mkdir(parents=True, exist_ok=True)
backup = config_backup_dir / "application.properties.phase3e2.bak"
backup.write_text(original, encoding="utf-8")
path.write_text("\n".join(lines) + "\n", encoding="utf-8")
print(f"Matrix26 backup settings updated: {path}")
print(f"Previous configuration preserved at: {backup}")
PY
