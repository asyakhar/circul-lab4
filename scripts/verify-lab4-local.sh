#!/usr/bin/env bash
set -euo pipefail

SOAP_WSDL_URL="${SOAP_WSDL_URL:-http://127.0.0.1:8180/ticket-service/ws/tickets.wsdl}"
MULE_API_URL="${MULE_API_URL:-http://127.0.0.1:8083/api/v1}"
MULE_HEALTH_URL="${MULE_HEALTH_URL:-http://127.0.0.1:8083/health}"
BOOKING_API_URL="${BOOKING_API_URL:-http://127.0.0.1:8082/booking}"

for command_name in curl python3; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "Не найдена команда '$command_name'." >&2
        exit 1
    fi
done

echo "Проверка WSDL и Mule health..."
curl -fsS "$SOAP_WSDL_URL" >/dev/null
curl -fsS "$MULE_HEALTH_URL" >/dev/null

ticket_json='{"name":"Lab 4 integration check","coordinates":{"x":10.5,"y":20},"price":2500.5,"comment":"temporary Mule SOAP check","type":"USUAL"}'
encoded_ticket="$(python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.stdin.read().strip(), safe=""))' <<<"$ticket_json")"

echo "Проверка client-compatible REST -> Mule -> SOAP..."
create_response="$(curl -fsS -X POST "$MULE_API_URL/tickets?ticket=$encoded_ticket")"
ticket_id="$(python3 -c 'import json, sys; print(json.load(sys.stdin)["id"])' <<<"$create_response")"
person_id="$(date +%s)"

cleanup() {
    curl -fsS -o /dev/null -X DELETE \
        "$BOOKING_API_URL/person/$person_id/cancel" 2>/dev/null || true
    curl -fsS -o /dev/null -X DELETE \
        "$MULE_API_URL/tickets/$ticket_id" 2>/dev/null || true
}
trap cleanup EXIT

curl -fsS "$MULE_API_URL/tickets/$ticket_id" \
    | python3 -c 'import json, sys; data=json.load(sys.stdin); assert data["name"] == "Lab 4 integration check"'

echo "Проверка SOAP Fault -> прежняя HTTP-ошибка..."
fault_response="$(curl -sS -w $'\n%{http_code}' "$MULE_API_URL/tickets/-1")"
fault_status="${fault_response##*$'\n'}"
fault_body="${fault_response%$'\n'*}"
if [[ "$fault_status" != "422" ]]; then
    echo "Ожидался HTTP 422, получен $fault_status: $fault_body" >&2
    exit 1
fi
python3 -c 'import json, sys; data=json.load(sys.stdin); assert data["status"] == 422; assert data["violations"][0]["field"] == "id"' <<<"$fault_body"

echo "Проверка Booking Service -> Mule -> SOAP..."
curl -fsS -o /dev/null -X POST \
    "$BOOKING_API_URL/sell/$ticket_id/$person_id/100"

echo "Обе интеграционные цепочки работают. Тестовые данные удаляются."
