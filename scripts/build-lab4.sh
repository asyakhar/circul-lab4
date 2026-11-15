#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Сборка и тесты SOAP Ticket Service..."
mvn -f "$PROJECT_DIR/ticket-service/pom.xml" clean test package

echo
echo "Сборка и тесты Booking Service без изменения его API..."
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PROJECT_DIR/.run/gradle}"
"$PROJECT_DIR/booking-service/gradlew" \
    -p "$PROJECT_DIR/booking-service" \
    clean test bootJar

echo
echo "Сборка REST-прослойки Mule..."
mvn -f "$PROJECT_DIR/mule-rest-adapter/pom.xml" clean package

echo
echo "Локальные артефакты лабораторной №4 собраны:"
echo "$PROJECT_DIR/ticket-service/target/ticket-service-1.0.0.war"
echo "$PROJECT_DIR/booking-service/build/libs/booking-service-1.0.0.jar"
echo "$PROJECT_DIR/mule-rest-adapter/target/ticket-rest-adapter-1.0.0-mule-application.jar"
