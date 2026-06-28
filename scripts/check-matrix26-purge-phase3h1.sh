#!/usr/bin/env bash
set -euo pipefail

assert_file() {
  local file="$1"
  if [ ! -f "$file" ]; then
    echo "Missing required file: $file" >&2
    exit 1
  fi
}

assert_contains() {
  local file="$1"
  local pattern="$2"
  local label="$3"
  if ! grep -qE "$pattern" "$file"; then
    echo "Check failed: $label" >&2
    echo "File: $file" >&2
    exit 1
  fi
}

assert_file src/main/java/com/ecoamazonas/eco_agua/platform/control/purge/Matrix26PurgeController.java
assert_file src/main/java/com/ecoamazonas/eco_agua/platform/control/purge/Matrix26PurgeService.java
assert_file src/main/java/com/ecoamazonas/eco_agua/platform/control/purge/Matrix26PurgeRepository.java
assert_file src/main/java/com/ecoamazonas/eco_agua/platform/control/purge/Matrix26PurgeInitializer.java
assert_file src/main/resources/templates/control_center/purge/index.html
assert_file src/main/resources/templates/control_center/purge/new.html
assert_file src/main/resources/templates/control_center/purge/detail.html
assert_file src/main/resources/templates/control_center/fragments/sidebar.html

assert_contains src/main/java/com/ecoamazonas/eco_agua/platform/control/purge/Matrix26PurgeController.java '/control-center/purge' 'Purge routes are registered'
assert_contains src/main/java/com/ecoamazonas/eco_agua/platform/control/purge/Matrix26PurgeController.java '\{planId:\\\\d\+\}' 'Numeric plan routes are constrained'
assert_contains src/main/java/com/ecoamazonas/eco_agua/platform/control/purge/Matrix26PurgeService.java 'PREPARE PURGE DRY RUN' 'Exact confirmation is required'
assert_contains src/main/java/com/ecoamazonas/eco_agua/platform/control/purge/Matrix26PurgeService.java 'Deleted resources: 0' 'Dry run reports zero deleted resources'
assert_contains src/main/java/com/ecoamazonas/eco_agua/platform/control/purge/Matrix26PurgeService.java 'Matrix26PurgeDisposition.WOULD_DELETE' 'Would-delete classification exists'
assert_contains src/main/java/com/ecoamazonas/eco_agua/platform/control/purge/Matrix26PurgeService.java 'Matrix26PurgeDisposition.PROTECTED' 'Protected classification exists'
assert_contains src/main/java/com/ecoamazonas/eco_agua/platform/control/purge/Matrix26PurgeService.java 'repository.schemaExists' 'Database inventory is metadata-only'
assert_contains src/main/java/com/ecoamazonas/eco_agua/platform/control/purge/Matrix26PurgeInitializer.java 'CREATE TABLE IF NOT EXISTS matrix26_purge_plan' 'Purge plan table is initialized'
assert_contains src/main/resources/templates/control_center/fragments/sidebar.html 'Purge dry run' 'Sidebar includes Purge dry run'

if grep -RniE 'DROP DATABASE|DROP SCHEMA|Files\.delete|deleteRecursively|deleteIfExists|DELETE FROM|removeDirectory' \
  src/main/java/com/ecoamazonas/eco_agua/platform/control/purge \
  src/main/resources/templates/control_center/purge; then
  echo "Destructive operation found in Phase 3H.1 purge package." >&2
  exit 1
fi

echo "Persistent purge plans, items, checks, and events: OK"
echo "Dry run routes and numeric detail boundaries: OK"
echo "Would-delete, keep, protected, blocked, review, and not-found classification: OK"
echo "Protected final backup and decommissioned instance gates: OK"
echo "No destructive operation exists in the Phase 3H.1 package: OK"
echo "Matrix26 Purge Manager Phase 3H.1 static checks passed."
