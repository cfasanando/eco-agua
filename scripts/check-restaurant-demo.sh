#!/usr/bin/env bash
set -euo pipefail

for URL in \
  "http://localhost:8084/login" \
  "http://localhost:8084/admin/restaurant" \
  "http://localhost:8084/admin/restaurant/dashboard" \
  "http://localhost:8084/admin/restaurant/tables" \
  "http://localhost:8084/admin/restaurant/orders/new" \
  "http://localhost:8084/admin/restaurant/kitchen" \
  "http://localhost:8084/restaurant/menu"
do
  echo "===== $URL ====="
  curl -I -L --max-time 10 "$URL" || true
  echo ""
done
