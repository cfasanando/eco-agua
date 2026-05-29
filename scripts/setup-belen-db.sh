#!/usr/bin/env bash
set -e

SOURCE_DB="${SOURCE_DB:-eco_agua_dev}"
TARGET_DB="${TARGET_DB:-productos_selva_belen}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"

find_mysql_tool() {
  local tool="$1"
  local candidates=(
    "/c/xampp/mysql/bin/${tool}.exe"
    "/c/laragon/bin/mysql/mysql-8.*/bin/${tool}.exe"
    "/c/laragon/bin/mysql/mysql-*/bin/${tool}.exe"
    "/c/Program Files/MySQL/MySQL Server 8.*/bin/${tool}.exe"
    "/c/Program Files/MariaDB */bin/${tool}.exe"
  )

  if command -v "$tool" >/dev/null 2>&1; then
    command -v "$tool"
    return 0
  fi

  for candidate in "${candidates[@]}"; do
    for match in $candidate; do
      if [ -f "$match" ]; then
        echo "$match"
        return 0
      fi
    done
  done

  return 1
}

MYSQL_BIN="$(find_mysql_tool mysql || true)"
MYSQLDUMP_BIN="$(find_mysql_tool mysqldump || true)"

if [ -z "$MYSQL_BIN" ] || [ -z "$MYSQLDUMP_BIN" ]; then
  echo "MySQL client tools were not found."
  echo "Install MySQL client tools or add mysql.exe and mysqldump.exe to PATH."
  echo "Common paths checked: XAMPP, Laragon, MySQL Server, MariaDB."
  exit 1
fi

AUTH_ARGS=(-u "$MYSQL_USER")
if [ -n "$MYSQL_PASSWORD" ]; then
  AUTH_ARGS+=(-p"$MYSQL_PASSWORD")
fi

BACKUP_FILE="/tmp/${SOURCE_DB}_to_${TARGET_DB}.sql"

echo "Using mysql: $MYSQL_BIN"
echo "Using mysqldump: $MYSQLDUMP_BIN"
echo "Source DB: $SOURCE_DB"
echo "Target DB: $TARGET_DB"

"$MYSQL_BIN" "${AUTH_ARGS[@]}" -e "CREATE DATABASE IF NOT EXISTS ${TARGET_DB} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
"$MYSQLDUMP_BIN" "${AUTH_ARGS[@]}" "$SOURCE_DB" > "$BACKUP_FILE"
"$MYSQL_BIN" "${AUTH_ARGS[@]}" "$TARGET_DB" < "$BACKUP_FILE"

if [ -f "database/belen-platform-settings.sql" ]; then
  "$MYSQL_BIN" "${AUTH_ARGS[@]}" "$TARGET_DB" < database/belen-platform-settings.sql
fi

echo "Belen database is ready: $TARGET_DB"
