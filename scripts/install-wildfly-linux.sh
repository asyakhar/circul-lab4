#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

WILDFLY_VERSION="41.0.1.Final"
WILDFLY_NAME="wildfly-ee-10-${WILDFLY_VERSION}"
WILDFLY_DIR="$LAB2_DIR/wildfly/$WILDFLY_NAME"

DOWNLOAD_URL="https://github.com/wildfly/wildfly/releases/download/${WILDFLY_VERSION}/${WILDFLY_NAME}.tar.gz"
CHECKSUM_URL="${DOWNLOAD_URL}.sha1"

if [[ "$(uname -s)" != "Linux" ]]; then
    echo "Этот скрипт предназначен для Linux."
    exit 1
fi

for command_name in curl tar sha1sum; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "Не найдена команда: $command_name"
        exit 1
    fi
done

if [[ -x "$WILDFLY_DIR/bin/standalone.sh" ]]; then
    echo "WildFly уже установлен:"
    echo "$WILDFLY_DIR"
    exit 0
fi

if [[ -e "$WILDFLY_DIR" ]]; then
    echo "Каталог WildFly существует, но установка выглядит неполной:"
    echo "$WILDFLY_DIR"
    echo "Удалите этот каталог вручную и запустите скрипт ещё раз."
    exit 1
fi

TEMP_DIR="$(mktemp -d)"
ARCHIVE_FILE="$TEMP_DIR/${WILDFLY_NAME}.tar.gz"
CHECKSUM_FILE="$TEMP_DIR/${WILDFLY_NAME}.tar.gz.sha1"

cleanup() {
    rm -rf "$TEMP_DIR"
}

trap cleanup EXIT

mkdir -p "$LAB2_DIR/wildfly"

echo "Скачивание WildFly EE10 ${WILDFLY_VERSION}..."
curl --fail --location --progress-bar \
    "$DOWNLOAD_URL" \
    --output "$ARCHIVE_FILE"

echo "Скачивание контрольной суммы..."
curl --fail --location --silent --show-error \
    "$CHECKSUM_URL" \
    --output "$CHECKSUM_FILE"

EXPECTED_HASH="$(awk '{print $1}' "$CHECKSUM_FILE")"
ACTUAL_HASH="$(sha1sum "$ARCHIVE_FILE" | awk '{print $1}')"

if [[ "$EXPECTED_HASH" != "$ACTUAL_HASH" ]]; then
    echo "Ошибка: контрольная сумма архива не совпала."
    exit 1
fi

echo "Контрольная сумма верна."
echo "Распаковка WildFly..."

tar -xzf "$ARCHIVE_FILE" -C "$LAB2_DIR/wildfly"

if [[ ! -x "$WILDFLY_DIR/bin/standalone.sh" ]]; then
    echo "Ошибка: WildFly распакован некорректно."
    exit 1
fi

echo
echo "WildFly успешно установлен:"
echo "$WILDFLY_DIR"
echo
echo "Теперь можно запускать:"
echo "./scripts/setup-wildfly-local.sh"