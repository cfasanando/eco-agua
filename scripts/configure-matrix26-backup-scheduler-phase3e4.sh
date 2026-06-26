#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="$ROOT/runtime-clients/matrix26_control/application.properties"
BACKUP_DIR="$ROOT/runtime-data/matrix26-control/maintenance"
BACKUP_FILE="$BACKUP_DIR/application.properties.phase3e4.bak"

if [ ! -f "$TARGET" ]; then
  echo "Matrix26 runtime configuration was not found: $TARGET" >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"
cp -f "$TARGET" "$BACKUP_FILE"

TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

grep -vE '^matrix26\.control-center\.backups\.(scheduling-enabled|scheduler-poll-milliseconds|scheduler-initial-delay-milliseconds|scheduler-grace-minutes|schedule-timezone)=' \
  "$TARGET" > "$TMP"

cat >> "$TMP" <<'PROPS'

# Matrix26 Backups Phase 3E.4
matrix26.control-center.backups.scheduling-enabled=true
matrix26.control-center.backups.scheduler-poll-milliseconds=60000
matrix26.control-center.backups.scheduler-initial-delay-milliseconds=15000
matrix26.control-center.backups.scheduler-grace-minutes=2
matrix26.control-center.backups.schedule-timezone=America/Lima
PROPS

mv -f "$TMP" "$TARGET"
trap - EXIT

echo "Matrix26 backup scheduler configuration updated:"
echo "$TARGET"
echo "Previous configuration preserved at:"
echo "$BACKUP_FILE"
