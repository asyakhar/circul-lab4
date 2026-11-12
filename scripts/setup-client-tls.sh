#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TLS_DIR="$LAB2_DIR/wildfly/tls"
CERT_FILE="$TLS_DIR/client-cert.pem"
KEY_FILE="$TLS_DIR/client-key.pem"
PUBLIC_CLIENT_HOST="${PUBLIC_CLIENT_HOST:-helios.cs.ifmo.ru}"

mkdir -p "$TLS_DIR"

if [[ -f "$CERT_FILE" && -f "$KEY_FILE" ]]; then
    if openssl x509 -in "$CERT_FILE" -noout -ext subjectAltName 2>/dev/null \
            | grep -Fq "DNS:$PUBLIC_CLIENT_HOST"; then
        echo "HTTPS-сертификат клиента уже существует."
        exit 0
    fi
    echo "Перевыпуск HTTPS-сертификата для $PUBLIC_CLIENT_HOST..."
fi

openssl req -x509 -newkey rsa:2048 -sha256 -nodes \
    -days 3650 \
    -subj "/CN=$PUBLIC_CLIENT_HOST" \
    -addext "subjectAltName=DNS:$PUBLIC_CLIENT_HOST,DNS:localhost,IP:127.0.0.1" \
    -keyout "$KEY_FILE" \
    -out "$CERT_FILE" \
    >/dev/null 2>&1

chmod 600 "$KEY_FILE"
echo "HTTPS-сертификат клиента создан в $TLS_DIR."
