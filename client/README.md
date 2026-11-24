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

Для лабораторной №4 клиент должен обращаться к Ticket Service через Mule.
Готовый локальный запуск выставляет нужные адреса автоматически:

```bash
./scripts/run-client-lab4-local.sh
```

## Отправка PDF-билета на почту

Локальный Flask-клиент показывает кнопку «На почту», когда заданы
`SMTP_HOST` и `SMTP_FROM`. Получатель вводит адрес в форме. Flask скачивает
PDF по существующему маршруту Mule и передаёт письмо с вложением SMTP-серверу.

Для демонстрации на Ubuntu можно запустить локальный почтовый ящик
[Mailpit](https://mailpit.axllent.org/docs/install/) через Docker:

```bash
sudo docker run --rm --name circul-mailpit \
  -p 127.0.0.1:1025:1025 \
  -p 127.0.0.1:8025:8025 \
  axllent/mailpit
```

На macOS Mailpit можно установить и запустить без Docker:

```bash
brew install mailpit
mailpit --smtp 127.0.0.1:1025 --listen 127.0.0.1:8025
```

При запущенных WildFly и Mule перезапустите клиент из корня проекта:

```bash
SMTP_HOST=127.0.0.1 SMTP_SECURITY=plain SMTP_FROM=tickets@example.test \
  CLIENT_PORT=5001 ./scripts/run-client-lab4-local.sh
```

На [странице клиента](https://localhost:5001) выберите билет, нажмите «На почту»
и введите, например, `demo@example.test`. Письмо с PDF появится в
[локальном ящике Mailpit](http://127.0.0.1:8025).

Для доставки на реальный адрес задайте `SMTP_HOST` и `SMTP_FROM` своего
почтового провайдера, а при необходимости также `SMTP_USERNAME` и
`SMTP_PASSWORD`. По умолчанию используется `SMTP_SECURITY=starttls` на порту
587. Для TLS сразу при подключении используйте `SMTP_SECURITY=ssl` (порт 465).
При необходимости переопределите порт через `SMTP_PORT`. Секреты передавайте
через окружение процесса.

Проверить почтовую логику без подключения к внешнему SMTP-серверу:

```bash
client/.venv/bin/python -m unittest discover -s client/tests -v
```

Окружение `client/.venv` автоматически создаётся при первом запуске клиента.

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
