#!/usr/bin/env bash
set -euo pipefail

PROFILE="${1:-}"

if [[ -z "$PROFILE" ]]; then
  echo "Usage: bash scripts/repair-matrix26-runtime-launcher.sh <runtime-profile>" >&2
  exit 1
fi

if [[ ! "$PROFILE" =~ ^[a-zA-Z0-9_-]+$ ]]; then
  echo "Invalid runtime profile: $PROFILE" >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="$ROOT_DIR/runtime-clients/$PROFILE"
CONFIG_FILE="$RUNTIME_DIR/application.properties"
RUN_SCRIPT="$RUNTIME_DIR/run.sh"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Runtime configuration was not found: $CONFIG_FILE" >&2
  exit 1
fi

cat > "$RUN_SCRIPT" <<EOF
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="\$(cd "\$(dirname "\${BASH_SOURCE[0]}")/../.." && pwd)"
CONFIG_FILE="\$ROOT_DIR/runtime-clients/$PROFILE/application.properties"

if [[ ! -f "\$CONFIG_FILE" ]]; then
  echo "Runtime configuration was not found: \$CONFIG_FILE" >&2
  exit 1
fi

if command -v cygpath >/dev/null 2>&1; then
  CONFIG_PATH="\$(cygpath -m "\$CONFIG_FILE")"
else
  CONFIG_PATH="\$CONFIG_FILE"
fi

cd "\$ROOT_DIR"

JAR="\$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*sources*' | head -n 1)"
if [[ -z "\$JAR" ]]; then
  echo "No application JAR found. Run: mvn clean -DskipTests package" >&2
  exit 1
fi

exec java -jar "\$JAR" --spring.config.additional-location="file:\${CONFIG_PATH}"
EOF

chmod +x "$RUN_SCRIPT"

echo "Runtime launcher repaired:"
echo "$RUN_SCRIPT"
