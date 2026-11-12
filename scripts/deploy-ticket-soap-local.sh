#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SERVER_BASE="${WILDFLY_TICKET_BASE:-$LAB_DIR/wildfly/servers/ticket}"
DEPLOYMENTS_DIR="$SERVER_BASE/deployments"
SOURCE_WAR="$LAB_DIR/ticket-service/target/ticket-service-1.0.0.war"
TARGET_WAR="$DEPLOYMENTS_DIR/ticket-service.war"

if [[ ! -f "$SOURCE_WAR" ]]; then
    echo "WAR не найден. Выполните: mvn -f ticket-service/pom.xml clean test package" >&2
    exit 1
fi

if [[ ! -d "$DEPLOYMENTS_DIR" ]]; then
    echo "Каталог deployments не найден: $DEPLOYMENTS_DIR" >&2
    echo "Укажите base directory WildFly через WILDFLY_TICKET_BASE." >&2
    exit 1
fi

rm -f "$TARGET_WAR.deployed" "$TARGET_WAR.failed" "$TARGET_WAR.isdeploying"
cp "$SOURCE_WAR" "$TARGET_WAR"
touch "$TARGET_WAR.dodeploy"

echo "SOAP Ticket Service передан WildFly для развёртывания."
echo "Ожидаемый WSDL: https://localhost:8543/ticket-service/ws/tickets.wsdl"
