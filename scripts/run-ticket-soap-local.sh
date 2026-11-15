#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WILDFLY_HOME="${WILDFLY_HOME:-$PROJECT_DIR/wildfly/wildfly-ee-10-41.0.1.Final}"
SERVER_BASE="${WILDFLY_TICKET_BASE:-$PROJECT_DIR/wildfly/servers/ticket}"

if [[ ! -x "$WILDFLY_HOME/bin/standalone.sh" ]]; then
    echo "WildFly не найден. Выполните ./scripts/setup-wildfly-local.sh." >&2
    exit 1
fi

if [[ ! -r "$SERVER_BASE/deployments/ticket-service.war" ]]; then
    echo "SOAP Ticket Service не развёрнут. Выполните ./scripts/deploy-ticket-soap-local.sh." >&2
    exit 1
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
    JAVA_PATH="$(readlink -f "$(command -v java)")"
    JAVA_HOME="$(dirname "$(dirname "$JAVA_PATH")")"
    export JAVA_HOME
fi

echo "SOAP endpoint: http://127.0.0.1:8180/ticket-service/ws"
echo "SOAP WSDL:    http://127.0.0.1:8180/ticket-service/ws/tickets.wsdl"

exec "$WILDFLY_HOME/bin/standalone.sh" \
    -Djboss.server.base.dir="$SERVER_BASE" \
    -Djboss.socket.binding.port-offset=100 \
    -Djboss.tx.node.id=ticket
