#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-.}"
BASE="$ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/restores"

required=(
  Matrix26InPlaceRestoreCandidate.java
  Matrix26InPlaceRestoreCheck.java
  Matrix26InPlaceRestoreController.java
  Matrix26InPlaceRestoreInitializer.java
  Matrix26InPlaceRestoreJob.java
  Matrix26InPlaceRestoreRepository.java
  Matrix26InPlaceRestoreService.java
  Matrix26InPlaceRestoreStatus.java
  Matrix26InPlaceRestoreStep.java
  Matrix26InPlaceRestoreStepStatus.java
  Matrix26InPlaceRestoreSummary.java
  Matrix26RestoreProperties.java
  Matrix26RestoreController.java
)

for file in "${required[@]}"; do
  test -f "$BASE/$file" || {
    echo "Missing required file: $BASE/$file" >&2
    exit 1
  }
done

grep -Fq '@GetMapping("/{id:\\d+}")' "$BASE/Matrix26RestoreController.java"
grep -Fq '@GetMapping("/{id:\\d+}")' "$BASE/Matrix26InPlaceRestoreController.java"
grep -Fq 'class Matrix26InPlaceRestoreService' "$BASE/Matrix26InPlaceRestoreService.java"
grep -Fq 'record Matrix26InPlaceRestoreJob' "$BASE/Matrix26InPlaceRestoreJob.java" || \
  grep -Fq 'class Matrix26InPlaceRestoreJob' "$BASE/Matrix26InPlaceRestoreJob.java"

echo "Complete in-place restore source set: OK"
echo "Static and numeric restore route boundaries: OK"
echo "Matrix26 Restore Manager Phase 3F.4b repair checks passed."
