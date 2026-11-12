#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
build() {
    local module="$1"
    echo "Сборка $module..."
    if [[ -n "${MAVEN_REPO_LOCAL:-}" ]]; then
        mvn "-Dmaven.repo.local=$MAVEN_REPO_LOCAL" \
            -f "$PROJECT_DIR/$module/pom.xml" clean package
    else
        mvn -f "$PROJECT_DIR/$module/pom.xml" clean package
    fi
}

build config-server
build eureka-server
build ticket-service
build api-gateway

echo "Сборка booking-service..."
"$PROJECT_DIR/booking-service/gradlew" \
    -p "$PROJECT_DIR/booking-service" \
    clean test bootJar

echo "Компоненты лабораторной №3 собраны."
