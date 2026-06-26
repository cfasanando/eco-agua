#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="$ROOT/runtime-clients/matrix26_control/application.properties"
CONFIG_BACKUP_DIR="$ROOT/runtime-data/matrix26-control/maintenance"
mkdir -p "$CONFIG_BACKUP_DIR"

if [[ ! -f "$TARGET" ]]; then
  echo "Matrix26 runtime configuration was not found: $TARGET" >&2
  exit 1
fi

python - "$TARGET" "$CONFIG_BACKUP_DIR" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
backup_dir = Path(sys.argv[2])
keys = (
    "matrix26.control-center.backups.encryption-enabled=",
    "matrix26.control-center.backups.master-key-environment=",
    "matrix26.control-center.backups.retention-enabled=",
)

original = path.read_text(encoding="utf-8")
lines = [line for line in original.splitlines() if not line.strip().startswith(keys)]
while lines and not lines[-1].strip():
    lines.pop()

lines.extend([
    "",
    "# Matrix26 Backups Phase 3E.3",
    "matrix26.control-center.backups.encryption-enabled=true",
    "matrix26.control-center.backups.master-key-environment=MATRIX26_BACKUP_MASTER_KEY",
    "matrix26.control-center.backups.retention-enabled=true",
])

backup_dir.mkdir(parents=True, exist_ok=True)
backup = backup_dir / "application.properties.phase3e3.bak"
backup.write_text(original, encoding="utf-8")
path.write_text("\n".join(lines) + "\n", encoding="utf-8")
print(f"Matrix26 backup security settings updated: {path}")
print(f"Previous configuration preserved at: {backup}")
print("No encryption key was written to application.properties.")
PY
