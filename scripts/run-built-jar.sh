#!/usr/bin/env bash
set -euo pipefail

PROFILE="${1:-}"
JAR_PATH="${2:-target/eco-agua-0.0.1-SNAPSHOT.jar}"

if [[ -z "$PROFILE" ]]; then
  echo "Usage: bash scripts/run-built-jar.sh <aguaeco|belen> [path-to-jar]"
  exit 1
fi

if [[ ! -f "$JAR_PATH" ]]; then
  echo "JAR not found: $JAR_PATH"
  echo "Build it first with: mvn -DskipTests package"
  exit 1
fi

java -jar "$JAR_PATH" --spring.profiles.active="$PROFILE"
