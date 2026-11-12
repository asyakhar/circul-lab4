#!/usr/bin/env bash
set -euo pipefail

HELIOS_USER="${HELIOS_USER:-${1:-s409792}}"
HELIOS_HOST="${HELIOS_HOST:-helios.cs.ifmo.ru}"
HELIOS_KEY="${HELIOS_KEY:-}"

echo "Открываю SSH-туннель к фронту на Helios..."
echo "После подключения откройте https://localhost:5080 в браузере."
echo "Терминал должен оставаться открытым; для остановки нажмите Ctrl+C."
echo "Подключение: $HELIOS_USER@$HELIOS_HOST"

if [[ -n "$HELIOS_KEY" ]]; then
    exec ssh -N \
        -o ExitOnForwardFailure=yes \
        -o ServerAliveInterval=30 \
        -o ServerAliveCountMax=3 \
        -i "$HELIOS_KEY" \
        -p 2222 \
        -L 5080:127.0.0.1:18230 \
        "$HELIOS_USER@$HELIOS_HOST"
else
    exec ssh -N \
        -o ExitOnForwardFailure=yes \
        -o ServerAliveInterval=30 \
        -o ServerAliveCountMax=3 \
        -p 2222 \
        -L 5080:127.0.0.1:18230 \
        "$HELIOS_USER@$HELIOS_HOST"
fi
