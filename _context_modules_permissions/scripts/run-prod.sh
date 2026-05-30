#!/usr/bin/env bash
set -e

# Run in PROD profile (simulated locally)
# Put your prod password in an env var before running:
# export DB_PASS="your_password"
: "${DB_PASS:?DB_PASS is required. Example: export DB_PASS='your_password'}"

mvn spring-boot:run -Dspring-boot.run.profiles=prod
