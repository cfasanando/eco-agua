#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8084}"

check_url() {
  local path="$1"
  shift
  local status
  local expected

  status="$(curl -sS -o /dev/null -w '%{http_code}' "$BASE_URL$path")"
  printf '%-55s %s\n' "$path" "$status"

  for expected in "$@"; do
    if [[ "$status" == "$expected" ]]; then
      return 0
    fi
  done

  echo "Unexpected HTTP status for $path: $status. Expected one of: $*" >&2
  return 1
}

echo "Checking Restaurant public endpoints at $BASE_URL"
check_url "/restaurant/menu" 200 302
check_url "/login" 200

echo
echo "Protected admin endpoints should redirect anonymous users to login"
check_url "/admin/restaurant/dashboard" 302 303
check_url "/admin/restaurant/cash-sessions" 302 303
check_url "/admin/restaurant/reports" 302 303

echo "Restaurant endpoint checks passed. Complete authenticated role tests manually."
