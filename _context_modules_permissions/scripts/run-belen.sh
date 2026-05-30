#!/usr/bin/env bash
set -e

# Run Productos de la Selva Belen local profile
mvn spring-boot:run -Dspring-boot.run.profiles=belen
