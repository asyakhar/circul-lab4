#!/usr/bin/env bash
set -euo pipefail

CLIENT_PORT="${CLIENT_PORT:-18230}"
JAEGER_UI_URL="${JAEGER_UI_URL:-http://127.0.0.1:16686}"
CLIENT_URL="https://127.0.0.1:$CLIENT_PORT"

TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT

person_id="$(date +%s)"
sell_status=""
ticket_id=""

for attempt in {1..100}; do
    ticket_json="{\"name\":\"Jaeger demo $attempt\",\"coordinates\":{\"x\":1,\"y\":1},\"price\":100,\"comment\":\"distributed trace\",\"type\":\"VIP\"}"

    echo "Создаю демонстрационный билет через Flask -> Gateway -> Ticket Service..."
    create_status="$(curl -kSs -o "$TEMP_DIR/ticket.json" -w '%{http_code}' \
        --request POST --get --data-urlencode "ticket=$ticket_json" \
        "$CLIENT_URL/api/ticket/tickets")"
    if [[ "$create_status" != "201" ]]; then
        echo "Не удалось создать билет: HTTP $create_status" >&2
        cat "$TEMP_DIR/ticket.json" >&2
        exit 1
    fi

    ticket_id="$(python3 -c \
        'import json,sys; print(json.load(open(sys.argv[1], encoding="utf-8"))["id"])' \
        "$TEMP_DIR/ticket.json")"

    echo "Продаю билет через Flask -> Gateway -> HAProxy -> Booking -> Gateway -> Ticket..."
    sell_status="$(curl -kSs -D "$TEMP_DIR/headers.txt" -o "$TEMP_DIR/sell.json" \
        -w '%{http_code}' --request POST \
        "$CLIENT_URL/api/booking/sell/$ticket_id/$person_id/100")"

    if [[ "$sell_status" == "204" ]]; then
        break
    fi
    if [[ "$sell_status" == "409" ]]; then
        echo "Ticket ID $ticket_id уже есть в общем Booking Store, пробую следующий."
        continue
    fi

    echo "Продажа завершилась с HTTP $sell_status" >&2
    cat "$TEMP_DIR/sell.json" >&2
    exit 1
done

trace_id="$(sed -n 's/^X-Trace-Id:[[:space:]]*//Ip' "$TEMP_DIR/headers.txt" | tr -d '\r' | tail -1)"
booking_instance="$(sed -n 's/^X-Booking-Instance:[[:space:]]*//Ip' "$TEMP_DIR/headers.txt" | tr -d '\r' | tail -1)"

if [[ "$sell_status" != "204" ]]; then
    echo "Не удалось подобрать свободный Ticket ID за 100 попыток." >&2
    exit 1
fi
if [[ -z "$trace_id" ]]; then
    echo "Ответ не содержит X-Trace-Id: проверьте client.log." >&2
    exit 1
fi

echo
echo "Готово:"
echo "  Ticket ID:          $ticket_id"
echo "  Booking instance:   ${booking_instance:-не определён}"
echo "  Trace ID:           $trace_id"
echo "  Открыть трассировку: $JAEGER_UI_URL/trace/$trace_id"
echo
echo "Повторите скрипт: HAProxy должен выбрать следующий экземпляр Booking."
