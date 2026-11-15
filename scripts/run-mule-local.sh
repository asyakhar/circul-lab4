#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
MULE_PORT="${MULE_PORT:-8083}"
SOAP_WSDL_URL="${SOAP_WSDL_URL:-http://127.0.0.1:8180/ticket-service/ws/tickets.wsdl}"
SOAP_ENDPOINT_URL="${SOAP_ENDPOINT_URL:-http://127.0.0.1:8180/ticket-service/ws}"
MULE_APP="$PROJECT_DIR/mule-rest-adapter/target/ticket-rest-adapter-1.0.0-mule-application.jar"

if [[ -z "${MULE_HOME:-}" || ! -x "$MULE_HOME/bin/mule" ]]; then
    echo "Укажите MULE_HOME с установленным Mule Runtime." >&2
    exit 1
fi

if [[ ! -r "$MULE_APP" ]]; then
    echo "Mule application не собран. Выполните ./scripts/build-lab4.sh." >&2
    exit 1
fi

mkdir -p "$MULE_HOME/apps"
cp "$MULE_APP" "$MULE_HOME/apps/ticket-rest-adapter.jar"

echo "Mule REST API: http://127.0.0.1:$MULE_PORT/api/v1"
echo "SOAP WSDL:     $SOAP_WSDL_URL"

if [[ "$(uname -s)" == "Darwin" && "$(uname -m)" == "arm64" ]]; then
    echo "Apple Silicon: запуск Mule напрямую через Java без Service Wrapper."
    JAVA_17_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
    JAVA_17_VERSION=""
    if [[ -x "$JAVA_17_HOME/bin/java" ]]; then
        JAVA_17_VERSION="$(
            "$JAVA_17_HOME/bin/java" -XshowSettings:properties -version 2>&1 \
                | awk -F= '/java.specification.version/ {gsub(/[[:space:]]/, "", $2); print $2; exit}'
        )"
    fi
    if [[ "$JAVA_17_VERSION" != "17" ]]; then
        echo "Для Mule Web Service Consumer требуется JDK 17." >&2
        echo "Установите её: brew install --cask corretto@17" >&2
        exit 1
    fi
    export MULE_HOME
    export JAVA_BIN="$JAVA_17_HOME/bin/java"
    export HTTP_HOST="127.0.0.1"
    export HTTP_PORT="$MULE_PORT"
    export SOAP_WSDL_URL
    export SOAP_ENDPOINT_URL
    exec "$SCRIPT_DIR/run-mule-kernel-direct.sh"
fi

exec "$MULE_HOME/bin/mule" \
    -M-Dhttp.host=127.0.0.1 \
    -M-Dhttp.port="$MULE_PORT" \
    -M-Dticket.soap.wsdl="$SOAP_WSDL_URL" \
    -M-Dticket.soap.endpoint="$SOAP_ENDPOINT_URL"
