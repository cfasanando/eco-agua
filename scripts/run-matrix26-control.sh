#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="$ROOT_DIR/runtime-clients/matrix26_control/application.properties"

if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "[ERROR] Matrix26 runtime configuration was not found: $CONFIG_FILE" >&2
    exit 1
fi

if command -v cygpath >/dev/null 2>&1; then
    CONFIG_PATH="$(cygpath -m "$CONFIG_FILE")"
else
    CONFIG_PATH="$CONFIG_FILE"
fi

cd "$ROOT_DIR"

echo "[INFO] Starting Matrix26 Control Center"
echo "[INFO] URL: http://localhost:8091"
echo "[INFO] Database: matrix26_platform_control"
echo "[INFO] Protected instances remain unchanged: 8081, 8082 and 8084"

mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.config.additional-location=file:${CONFIG_PATH}"
