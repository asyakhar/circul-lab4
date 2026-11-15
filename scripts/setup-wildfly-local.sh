#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

WILDFLY_HOME="${WILDFLY_HOME:-$LAB2_DIR/wildfly/wildfly-ee-10-41.0.1.Final}"
SERVERS_DIR="$LAB2_DIR/wildfly/servers"

sed_in_place() {
    local expression="$1"
    local file="$2"

    if [[ "$(uname -s)" == "Linux" ]]; then
        sed -i "$expression" "$file"
    else
        sed -i '' "$expression" "$file"
    fi
}

if [[ ! -x "$WILDFLY_HOME/bin/standalone.sh" ]]; then
    echo "WildFly не найден: $WILDFLY_HOME" >&2
    echo "Укажите путь через переменную WILDFLY_HOME." >&2
    exit 1
fi

create_server() {
    local name="$1"
    local base="$SERVERS_DIR/$name"

    mkdir -p "$base"/{configuration,data,deployments,log,tmp}

    if [[ ! -f "$base/configuration/standalone.xml" ]]; then
        cp -R \
            "$WILDFLY_HOME/standalone/configuration/." \
            "$base/configuration/"

        # Keep both listeners. The local Mule adapter calls Ticket Service over
        # HTTP on port 8180, while HTTPS on 8543 remains available for a direct
        # browser/SoapUI demonstration.
    fi
}

create_server ticket
create_server booking

echo "Конфигурации WildFly созданы в $SERVERS_DIR"
