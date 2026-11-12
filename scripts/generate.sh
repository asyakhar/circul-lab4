#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CODEGEN_VERSION="3.0.71"
CODEGEN_JAR="$LAB2_DIR/tools/swagger-codegen-cli-$CODEGEN_VERSION.jar"
CODEGEN_URL="https://repo1.maven.org/maven2/io/swagger/codegen/v3/swagger-codegen-cli/$CODEGEN_VERSION/swagger-codegen-cli-$CODEGEN_VERSION.jar"

if [[ ! -f "$CODEGEN_JAR" ]]; then
  echo "Downloading Swagger Codegen $CODEGEN_VERSION..."
  curl --fail --location "$CODEGEN_URL" --output "$CODEGEN_JAR"
fi

node "$SCRIPT_DIR/prepare-specs.cjs"

if [[ ( -d "$LAB2_DIR/ticket-service" || -d "$LAB2_DIR/booking-service" ) && "${REGENERATE:-0}" != "1" ]]; then
  echo "Server projects already exist. Regeneration overwrites generated files."
  echo "Run REGENERATE=1 bash scripts/generate.sh only when this is intentional."
  exit 1
fi

rm -rf "$LAB2_DIR/ticket-service" "$LAB2_DIR/booking-service"

java -jar "$CODEGEN_JAR" generate \
  -i "$LAB2_DIR/specs/ticket-service.openapi.json" \
  -l spring \
  --library spring-boot3 \
  -o "$LAB2_DIR/ticket-service" \
  --api-package ru.ifmo.soa.ticket.api \
  --model-package ru.ifmo.soa.ticket.model \
  --invoker-package ru.ifmo.soa.ticket \
  --group-id ru.ifmo.soa \
  --artifact-id ticket-service \
  --artifact-version 1.0.0 \
  --additional-properties delegatePattern=true,useTags=true,useBeanValidation=true

java -jar "$CODEGEN_JAR" generate \
  -i "$LAB2_DIR/specs/booking-service.openapi.json" \
  -l jaxrs-resteasy \
  -o "$LAB2_DIR/booking-service" \
  --api-package ru.ifmo.soa.booking.api \
  --model-package ru.ifmo.soa.booking.model \
  --invoker-package ru.ifmo.soa.booking \
  --group-id ru.ifmo.soa \
  --artifact-id booking-service \
  --artifact-version 1.0.0 \
  --additional-properties jakarta=true,java11=true,dateLibrary=java11,useBeanValidation=true,generateJbossDeploymentDescriptor=true

node "$SCRIPT_DIR/adapt-generated.cjs"

echo "Generated ticket-service and booking-service with Swagger Codegen $CODEGEN_VERSION."
