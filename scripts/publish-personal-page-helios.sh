#!/usr/bin/env bash
set -euo pipefail

PUBLIC_URL="${1:-}"
PUBLIC_PATH="${PUBLIC_PATH:-circul}"
PUBLIC_DIR="$HOME/public_html/$PUBLIC_PATH"
INDEX_FILE="$PUBLIC_DIR/index.html"

if [[ ! "$PUBLIC_URL" =~ ^https://[a-z0-9-]+\.trycloudflare\.com$ ]]; then
    echo "Некорректный адрес публичного туннеля: $PUBLIC_URL" >&2
    exit 1
fi

case "$PUBLIC_PATH" in
    (*[!A-Za-z0-9._-]*|'')
        echo "Некорректный каталог public_html: $PUBLIC_PATH" >&2
        exit 1
        ;;
esac

mkdir -p "$PUBLIC_DIR"

if [[ -f "$INDEX_FILE" && ! -f "$PUBLIC_DIR/index.previous.html" ]]; then
    cp "$INDEX_FILE" "$PUBLIC_DIR/index.previous.html"
fi

tmp_file="$PUBLIC_DIR/.index.html.tmp.$$"
cat >"$tmp_file" <<EOF
<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ЦИРКуль — лабораторная работа №3</title>
    <style>
        html, body, iframe { width: 100%; height: 100%; margin: 0; border: 0; }
        body { overflow: hidden; background: #130812; }
        iframe { display: block; }
        .fallback { position: fixed; inset: auto 1rem 1rem; color: white; font: 14px sans-serif; }
        .fallback a { color: #ffd56a; }
    </style>
</head>
<body>
    <iframe
        src="$PUBLIC_URL"
        title="ЦИРКуль — лабораторная работа №3"
        allow="clipboard-read; clipboard-write"
        referrerpolicy="no-referrer"
    ></iframe>
    <noscript class="fallback">
        Для работы приложения включите JavaScript или
        <a href="$PUBLIC_URL">откройте приложение напрямую</a>.
    </noscript>
</body>
</html>
EOF

mv "$tmp_file" "$INDEX_FILE"
chmod 755 "$HOME/public_html" "$PUBLIC_DIR" 2>/dev/null || true
chmod 644 "$INDEX_FILE"

echo "Персональная страница опубликована:"
echo "https://se.ifmo.ru/~${USER}/$PUBLIC_PATH/"
