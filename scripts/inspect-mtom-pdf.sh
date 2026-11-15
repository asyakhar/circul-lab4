#!/usr/bin/env bash
set -euo pipefail

SOAP_ENDPOINT_URL="${SOAP_ENDPOINT_URL:-http://127.0.0.1:8180/ticket-service/ws}"
TICKET_ID="${1:-1}"
TEMP_DIR="$(mktemp -d)"
HEADERS_FILE="$TEMP_DIR/headers.txt"
RESPONSE_FILE="$TEMP_DIR/response.bin"

cleanup() {
    rm -rf "$TEMP_DIR"
}
trap cleanup EXIT

request="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:t=\"http://soa.ifmo.ru/ticket/ws\"><soapenv:Header/><soapenv:Body><t:downloadTicketRequest><t:id>$TICKET_ID</t:id></t:downloadTicketRequest></soapenv:Body></soapenv:Envelope>"

curl -fsS \
    -D "$HEADERS_FILE" \
    -o "$RESPONSE_FILE" \
    -H 'Content-Type: text/xml; charset=utf-8' \
    --data-binary "$request" \
    "$SOAP_ENDPOINT_URL"

echo "HTTP-заголовок SOAP-ответа:"
grep -i '^Content-Type:' "$HEADERS_FILE"
echo
echo "Признаки MTOM/XOP внутри multipart-ответа:"
LC_ALL=C grep -aEo \
    'xop:Include[^>]*|Content-Type: application/pdf|%PDF-[0-9.]+' \
    "$RESPONSE_FILE" \
    | head -n 4
