#!/usr/bin/env bash
set -euo pipefail

SOURCE_DB="${1:-restaurante_buen_sabor}"
TARGET_DB="${2:-eco_agua_modular_lab}"
PROFILE="${3:-modular_lab}"
PORT="${4:-8085}"
RESET_LAB="${RESET_LAB:-0}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_CONFIG="$PROJECT_ROOT/runtime-clients/demo_restaurante_buen_sabor/application.properties"
TARGET_DIR="$PROJECT_ROOT/runtime-clients/$PROFILE"
TARGET_CONFIG="$TARGET_DIR/application.properties"
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"; unset MYSQL_PWD' EXIT

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Required command not found: $1" >&2
        exit 1
    fi
}

validate_identifier() {
    if [[ ! "$1" =~ ^[A-Za-z][A-Za-z0-9_]*$ ]]; then
        echo "Invalid MySQL identifier: $1" >&2
        exit 1
    fi
}

require_command mysql
require_command mysqldump
require_command sed
validate_identifier "$SOURCE_DB"
validate_identifier "$TARGET_DB"

read -rsp "MySQL root password, press Enter if empty: " MYSQL_ROOT_PASSWORD
echo
export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"

SOURCE_EXISTS="$(mysql -N -B -u root -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = '$SOURCE_DB';")"
if [[ "$SOURCE_EXISTS" != "1" ]]; then
    echo "Source database does not exist: $SOURCE_DB" >&2
    exit 1
fi

TARGET_EXISTS="$(mysql -N -B -u root -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = '$TARGET_DB';")"
if [[ "$TARGET_EXISTS" == "1" && "$RESET_LAB" != "1" ]]; then
    echo "Target database already exists: $TARGET_DB" >&2
    echo "Run again with RESET_LAB=1 only if this disposable lab database can be recreated." >&2
    exit 1
fi

if [[ "$TARGET_EXISTS" == "1" ]]; then
    mysql -u root -e "DROP DATABASE \`$TARGET_DB\`;"
fi
mysql -u root -e "CREATE DATABASE \`$TARGET_DB\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

SCHEMA_DUMP="$TEMP_DIR/schema.sql"
mysqldump -u root --no-data --skip-comments --routines=false --triggers=false "$SOURCE_DB" > "$SCHEMA_DUMP"
mysql -u root "$TARGET_DB" < "$SCHEMA_DUMP"

CORE_TABLES=(
    platform_setting
    platform_module_catalog
    roles
    permission
    role_permission
    user
    user_roles
)

for TABLE_NAME in "${CORE_TABLES[@]}"; do
    TABLE_EXISTS="$(mysql -N -B -u root -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$SOURCE_DB' AND table_name = '$TABLE_NAME';")"
    if [[ "$TABLE_EXISTS" == "1" ]]; then
        mysqldump -u root --no-create-info --skip-triggers --single-transaction "$SOURCE_DB" "$TABLE_NAME" \
            | mysql -u root "$TARGET_DB"
    fi
done

DROP_SQL="$TEMP_DIR/drop-restaurant.sql"
{
    echo "SET FOREIGN_KEY_CHECKS=0;"
    mysql -N -B -u root -e "SELECT CONCAT('DROP TABLE IF EXISTS \\`', table_name, '\\`;') FROM information_schema.tables WHERE table_schema = '$TARGET_DB' AND table_name LIKE 'restaurant\\_%' ESCAPE '\\\\' ORDER BY table_name DESC;"
    echo "SET FOREIGN_KEY_CHECKS=1;"
} > "$DROP_SQL"
mysql -u root "$TARGET_DB" < "$DROP_SQL"

mysql -u root "$TARGET_DB" <<'SQL'
DROP TABLE IF EXISTS platform_module_installation;
INSERT INTO platform_setting (variable, value, type, category, description)
VALUES ('module.restaurant.enabled', 'false', 'boolean', 'system_modules', 'Restaurant module runtime flag')
ON DUPLICATE KEY UPDATE value = 'false';
SQL

if [[ ! -f "$SOURCE_CONFIG" ]]; then
    echo "Runtime source configuration was not found: $SOURCE_CONFIG" >&2
    exit 1
fi

mkdir -p "$TARGET_DIR"
sed -E \
    -e "s/^server\.port=.*/server.port=$PORT/" \
    -e "s#^(spring\.datasource\.url=jdbc:mysql://[^/]+/)[^?]+#\\1$TARGET_DB#" \
    "$SOURCE_CONFIG" > "$TARGET_CONFIG"

if ! grep -q '^server.port=' "$TARGET_CONFIG"; then
    printf '\nserver.port=%s\n' "$PORT" >> "$TARGET_CONFIG"
fi
if grep -q '^ecoagua\.modules\.installation-allowed=' "$TARGET_CONFIG"; then
    sed -i -E 's/^ecoagua\.modules\.installation-allowed=.*/ecoagua.modules.installation-allowed=true/' "$TARGET_CONFIG"
else
    printf '\necoagua.modules.installation-allowed=true\n' >> "$TARGET_CONFIG"
fi

cat > "$TARGET_DIR/README.txt" <<TXT
Disposable modular laboratory

Database: $TARGET_DB
Port: $PORT
Profile: $PROFILE

The database contains common structure and authentication/configuration data only.
Restaurant tables were removed and module.restaurant.enabled was set to false.
Use /admin/system-modules/installations to install Restaurant explicitly.
TXT

echo
echo "Modular lab prepared successfully."
echo "Database: $TARGET_DB"
echo "Runtime config: $TARGET_CONFIG"
echo "Port: $PORT"
echo
echo "Build the application, then run:"
echo "bash scripts/run-modular-lab.sh $PROFILE"
