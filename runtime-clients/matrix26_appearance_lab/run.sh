#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CONFIG_FILE="$ROOT_DIR/runtime-clients/matrix26_appearance_lab/application.properties"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Runtime configuration was not found: $CONFIG_FILE" >&2
  exit 1
fi

if command -v cygpath >/dev/null 2>&1; then
  CONFIG_PATH="$(cygpath -m "$CONFIG_FILE")"
else
  CONFIG_PATH="$CONFIG_FILE"
fi

cd "$ROOT_DIR"

JAR="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*sources*' | head -n 1)"
if [[ -z "$JAR" ]]; then
  echo "No application JAR found. Run: mvn clean -DskipTests package" >&2
  exit 1
fi

exec java -jar "$JAR" --spring.config.additional-location="file:${CONFIG_PATH}"
