#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="$ROOT_DIR/runtime-clients/matrix26_control/application.properties"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Matrix26 control runtime configuration was not found: $CONFIG_FILE" >&2
  echo "Nothing was changed. Apply the Matrix26 control runtime first, then run this script again." >&2
  exit 0
fi

backup_file="$ROOT_DIR/runtime-data/matrix26-control/maintenance/application.properties.phase3i2.bak"
mkdir -p "$(dirname "$backup_file")"
cp "$CONFIG_FILE" "$backup_file"

ensure_property() {
  local key="$1"
  local value="$2"
  if grep -qE "^[[:space:]]*${key}=" "$CONFIG_FILE"; then
    sed -i -E "s|^[[:space:]]*${key}=.*|${key}=${value}|" "$CONFIG_FILE"
  else
    printf '\n%s=%s\n' "$key" "$value" >> "$CONFIG_FILE"
  fi
}

ensure_property "matrix26.control-center.enabled" "true"

echo "Matrix26 Operations Phase 3I.2 configuration checked."
echo "Backup created at: $backup_file"
echo "No destructive setting was enabled or changed."
