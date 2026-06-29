#!/usr/bin/env bash
set -euo pipefail

CONFIG_FILE="runtime-clients/matrix26_control/application.properties"
BACKUP_DIR="runtime-data/matrix26-control/maintenance"
BACKUP_FILE="$BACKUP_DIR/application.properties.phase3i3.bak"

if [ ! -f "$CONFIG_FILE" ]; then
  echo "Configuration file not found: $CONFIG_FILE" >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"
cp "$CONFIG_FILE" "$BACKUP_FILE"

ensure_property() {
  local key="$1"
  local value="$2"

  if grep -qE "^[[:space:]]*${key//./\.}=" "$CONFIG_FILE"; then
    sed -i -E "s|^[[:space:]]*${key//./\.}=.*|$key=$value|" "$CONFIG_FILE"
  else
    printf '\n%s=%s\n' "$key" "$value" >> "$CONFIG_FILE"
  fi
}

ensure_property "matrix26.control-center.enabled" "true"

cat <<MSG
Matrix26 Operations Phase 3I.3 configuration checked.
Backup created: $BACKUP_FILE

This phase only seeds Matrix26 roles and permission definitions.
It does not enable purge, archive destruction, restores, backups, or runtime actions.
MSG
