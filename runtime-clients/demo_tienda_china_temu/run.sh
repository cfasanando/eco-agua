#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
CONFIG_FILE="$SCRIPT_DIR/application.properties"

if [ ! -f "$CONFIG_FILE" ]; then
  echo "[ERROR] Runtime config not found: $CONFIG_FILE"
  exit 1
fi

if command -v cygpath >/dev/null 2>&1; then
  CONFIG_PATH="$(cygpath -m "$CONFIG_FILE")"
else
  CONFIG_PATH="$CONFIG_FILE"
fi

cd "$ROOT_DIR"

echo "[INFO] Starting client from:"
echo "$CONFIG_PATH"

mvn spring-boot:run -Dspring-boot.run.arguments="--spring.config.additional-location=file:$CONFIG_PATH"
