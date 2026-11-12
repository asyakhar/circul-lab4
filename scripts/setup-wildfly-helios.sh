#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WILDFLY_HOME="$LAB2_DIR/wildfly/wildfly-ee-10-41.0.1.Final"
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

"$SCRIPT_DIR/setup-wildfly-local.sh"

for name in ticket booking; do
    config="$SERVERS_DIR/$name/configuration/standalone.xml"
    cp "$WILDFLY_HOME/standalone/configuration/standalone-microprofile.xml" "$config"
    sed_in_place '/<http-listener name="default"/d' "$config"
    sed_in_place 's/connector-ref="default"/connector-ref="https"/' "$config"
done

echo "Облегчённые конфигурации WildFly для Helios подготовлены."
