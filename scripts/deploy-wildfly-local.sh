#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SERVERS_DIR="$LAB2_DIR/wildfly/servers"

TICKET_WAR="$LAB2_DIR/ticket-service/target/ticket-service-1.0.0.war"
BOOKING_WAR="$LAB2_DIR/booking-service/build/libs/booking.war"

if [[ ! -f "$TICKET_WAR" || ! -f "$BOOKING_WAR" ]]; then
    echo "собери два варника" >&2
    exit 1
fi

rm -f "$SERVERS_DIR/ticket/deployments/ticket-service.war".*
rm -f "$SERVERS_DIR/booking/deployments/booking.war".*

cp "$TICKET_WAR" \
    "$SERVERS_DIR/ticket/deployments/ticket-service.war"
cp "$BOOKING_WAR" \
    "$SERVERS_DIR/booking/deployments/booking.war"

touch "$SERVERS_DIR/ticket/deployments/ticket-service.war.dodeploy"
touch "$SERVERS_DIR/booking/deployments/booking.war.dodeploy"

