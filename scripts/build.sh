#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Сборка Ticket Service..."
mvn -f "$LAB2_DIR/ticket-service/pom.xml" clean package -DskipTests

echo
echo "Сборка Booking Service..."
"$LAB2_DIR/booking-service/gradlew" \
    -p "$LAB2_DIR/booking-service" \
    clean test war

echo
echo "Оба сервиса успешно собраны:"
echo "$LAB2_DIR/ticket-service/target/ticket-service-1.0.0.war"
echo "$LAB2_DIR/booking-service/build/libs/booking.war"