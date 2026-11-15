#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
MULE_ARCHIVE="${MULE_ARCHIVE:-$PROJECT_DIR/mule-standalone.zip}"
MULE_HOME="${MULE_HOME:-$PROJECT_DIR/mule/runtime}"
MULE_LICENSE="${MULE_LICENSE:-}"

install_license() {
    if [[ -z "$MULE_LICENSE" ]]; then
        return
    fi
    if [[ ! -r "$MULE_LICENSE" ]]; then
        echo "Не найден файл лицензии Mule: $MULE_LICENSE" >&2
        exit 1
    fi
    "$MULE_HOME/bin/mule" -M-Xmx128m -installLicense "$MULE_LICENSE"
    echo "Лицензия Mule установлена."
}

if [[ -x "$MULE_HOME/bin/mule" ]]; then
    echo "Mule Runtime уже установлен: $MULE_HOME"
    install_license
    exit 0
fi

if [[ ! -r "$MULE_ARCHIVE" ]]; then
    echo "Не найден официальный архив Mule Runtime: $MULE_ARCHIVE" >&2
    echo "Запустите scripts/download-mule-kernel.sh локально и передайте архив" >&2
    echo "через MULE_ARCHIVE либо используйте deploy-lab4-helios.sh." >&2
    exit 1
fi

if ! command -v unzip >/dev/null 2>&1; then
    echo "Для установки Mule требуется команда unzip." >&2
    exit 1
fi

mkdir -p "$(dirname "$MULE_HOME")"
TEMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/mule-install.XXXXXX")"
trap 'rm -rf "$TEMP_DIR"' EXIT

echo "Распаковка Mule Runtime..."
unzip -q "$MULE_ARCHIVE" -d "$TEMP_DIR"

extracted_home=""
while IFS= read -r candidate; do
    if [[ -x "$candidate/bin/mule" ]]; then
        if [[ -n "$extracted_home" ]]; then
            echo "В архиве найдено несколько каталогов Mule Runtime." >&2
            exit 1
        fi
        extracted_home="$candidate"
    fi
done < <(find "$TEMP_DIR" -mindepth 1 -maxdepth 2 -type d -print)

if [[ -z "$extracted_home" ]]; then
    echo "Архив не похож на Mule standalone distribution: bin/mule не найден." >&2
    exit 1
fi

if [[ -e "$MULE_HOME" ]]; then
    echo "Каталог $MULE_HOME уже существует, но Mule в нём не найден." >&2
    echo "Удалите или переименуйте этот каталог вручную и повторите установку." >&2
    exit 1
fi

mv "$extracted_home" "$MULE_HOME"
echo "Mule Runtime установлен: $MULE_HOME"
install_license
