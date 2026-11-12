#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CLIENT_DIR="$LAB2_DIR/client"
VENV_DIR="$CLIENT_DIR/.venv"
TLS_DIR="$LAB2_DIR/wildfly/tls"

if ! command -v python3 >/dev/null 2>&1; then
    echo "Python 3 не найден." >&2
    exit 1
fi

if [[ ! -x "$VENV_DIR/bin/python" ]]; then
    echo "Создание виртуального окружения клиента..."
    python3 -m venv "$VENV_DIR"
fi

echo "Установка зависимостей клиента..."
"$VENV_DIR/bin/python" -m pip install --quiet \
    -r "$CLIENT_DIR/requirements.txt"

bash "$SCRIPT_DIR/setup-client-tls.sh"

echo "Клиент доступен по адресу https://localhost:${CLIENT_PORT:-5000}"
exec env \
    CLIENT_CERT_FILE="$TLS_DIR/client-cert.pem" \
    CLIENT_KEY_FILE="$TLS_DIR/client-key.pem" \
    "$VENV_DIR/bin/python" "$CLIENT_DIR/app.py"
