# Client application

Простое Flask-приложение для работы с Ticket Service и Booking Service.
Запросы к HTTPS-сервисам идут через Flask, поэтому самоподписанные
сертификаты и ограничения CORS не мешают работе браузера.

Интерфейс разделён на три микрофронтенда на нативных ES-модулях и Web Components:

- `static/mfe/shell` — общая оболочка, сообщения и цирковые эффекты;
- `static/mfe/ticket` — афиша и все операции Ticket Service;
- `static/mfe/booking` — продажа, отмена и печать билета.

Микрофронтенды общаются через браузерные `CustomEvent` и могут обновляться независимо.

Локальный запуск из корня проекта:

```bash
./scripts/start-client-local.sh
```

Адрес: `https://localhost:5000`. При первом открытии браузер попросит
подтвердить самоподписанный сертификат.

Для других адресов сервисов можно задать `TICKET_SERVICE_URL`,
`BOOKING_SERVICE_URL` и `CLIENT_PORT`.

Для запуска клиента по HTTPS задайте одновременно `CLIENT_CERT_FILE` и
`CLIENT_KEY_FILE` — пути к PEM-сертификату и его закрытому ключу. Имена
`CLIENT_TLS_CERT` и `CLIENT_TLS_KEY` также поддерживаются для совместимости.

На Helios `start-helios.sh` сначала ищет пользовательские файлы
`client/tls/frontend.crt` и `client/tls/frontend.key`. Если их нет, скрипт
`setup-client-tls.sh` автоматически создаёт самоподписанную пару в
`wildfly/tls/client-cert.pem` и `wildfly/tls/client-key.pem`.

SSH-туннель по умолчанию подключается под пользователем `s409792`. Другой
аккаунт можно передать аргументом или переменной окружения, например:

```bash
./scripts/tunnel-helios.sh s409792
HELIOS_USER=s409792 ./scripts/tunnel-helios.sh
```
