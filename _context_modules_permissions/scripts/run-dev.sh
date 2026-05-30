#!/usr/bin/env bash
set -e

# Run in DEV profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
