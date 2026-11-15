#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WILDFLY_HOME="${WILDFLY_HOME:-$PROJECT_DIR/wildfly/wildfly-ee-10-41.0.1.Final}"
SERVER_BASE="${WILDFLY_TICKET_BASE:-$PROJECT_DIR/wildfly/servers/ticket-lab4}"

if [[ ! -x "$WILDFLY_HOME/bin/standalone.sh" ]]; then
    echo "WildFly не найден: $WILDFLY_HOME" >&2
    echo "Сначала выполните ./scripts/install-wildfly-helios.sh." >&2
    exit 1
fi

mkdir -p "$SERVER_BASE"/{configuration,data,deployments,log,tmp}

if [[ ! -f "$SERVER_BASE/configuration/standalone.xml" ]]; then
    cp -R "$WILDFLY_HOME/standalone/configuration/." \
        "$SERVER_BASE/configuration/"
fi

echo "Конфигурация Ticket WildFly для лабораторной №4: $SERVER_BASE"
