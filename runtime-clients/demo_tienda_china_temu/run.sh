#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
CONFIG_FILE="$SCRIPT_DIR/application.properties"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "[ERROR] Runtime config not found: $CONFIG_FILE"
  exit 1
fi

if command -v cygpath >/dev/null 2>&1; then
  CONFIG_PATH="$(cygpath -m "$CONFIG_FILE")"
else
  CONFIG_PATH="$CONFIG_FILE"
fi

cd "$PROJECT_DIR"

echo "[INFO] Starting client runtime from: $CONFIG_PATH"

mvn spring-boot:run   -Dspring-boot.run.arguments="--spring.config.additional-location=file:$CONFIG_PATH --server.port=8083"
