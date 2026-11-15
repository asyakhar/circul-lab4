#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

MULE_VERSION="${MULE_VERSION:-4.10.0}"
MULE_ARCHIVE="${MULE_ARCHIVE:-$PROJECT_DIR/.run/deploy-cache/mule-standalone-$MULE_VERSION.zip}"
MULE_BASE_URL="https://repository-master.mulesoft.org/nexus/repository/public/org/mule/distributions/mule-standalone"
MULE_URL="${MULE_URL:-$MULE_BASE_URL/$MULE_VERSION/mule-standalone-$MULE_VERSION.zip}"

case "$MULE_VERSION" in
    4.10.0)
        EXPECTED_SHA256="18df4614ee2ffb7ac70cb50142df7741362ea623af2262e62467781b48f18cf2"
        ;;
    *)
        echo "Для Mule $MULE_VERSION контрольная сумма не зафиксирована в скрипте." >&2
        echo "Используйте проверенную версию 4.10.0 или обновите EXPECTED_SHA256." >&2
        exit 1
        ;;
esac

verify_checksum() {
    local actual
    if command -v sha256sum >/dev/null 2>&1; then
        actual="$(sha256sum "$MULE_ARCHIVE" | awk '{print $1}')"
    elif command -v shasum >/dev/null 2>&1; then
        actual="$(shasum -a 256 "$MULE_ARCHIVE" | awk '{print $1}')"
    else
        echo "Не найдена команда sha256sum или shasum." >&2
        exit 1
    fi

    if [[ "$actual" != "$EXPECTED_SHA256" ]]; then
        echo "Неверная SHA-256 у $MULE_ARCHIVE" >&2
        echo "ожидалась: $EXPECTED_SHA256" >&2
        echo "получена:  $actual" >&2
        exit 1
    fi
}

if [[ -r "$MULE_ARCHIVE" ]]; then
    verify_checksum
    echo "$MULE_ARCHIVE"
    exit 0
fi

if ! command -v curl >/dev/null 2>&1; then
    echo "Для скачивания Mule Kernel требуется curl." >&2
    exit 1
fi

mkdir -p "$(dirname "$MULE_ARCHIVE")"
partial_archive="$MULE_ARCHIVE.part"
trap 'rm -f "$partial_archive"' EXIT

echo "Скачивание официального Mule Kernel $MULE_VERSION..." >&2
curl -fL --retry 3 --connect-timeout 20 -o "$partial_archive" "$MULE_URL"
mv "$partial_archive" "$MULE_ARCHIVE"
verify_checksum

echo "$MULE_ARCHIVE"
