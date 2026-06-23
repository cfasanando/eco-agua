#!/usr/bin/env bash
set -euo pipefail

PROFILE="${1:-modular_lab}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="$PROJECT_ROOT/runtime-clients/$PROFILE/application.properties"

if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "Runtime configuration not found: $CONFIG_FILE" >&2
    echo "Run scripts/setup-modular-lab.sh first." >&2
    exit 1
fi

JAR="$(find "$PROJECT_ROOT/target" -maxdepth 1 -type f -name '*.jar' ! -name '*sources*' | head -n 1)"
if [[ -z "$JAR" ]]; then
    echo "Application JAR not found. Run mvn clean -DskipTests package first." >&2
    exit 1
fi

exec java -jar "$JAR" \
    --spring.config.additional-location="file:$CONFIG_FILE"
