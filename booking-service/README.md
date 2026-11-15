# Booking Service

Вызывающий сервис лабораторной №3. Он запускается как самостоятельное
Spring Boot-приложение с embedded Tomcat и сохраняет JAX-RS API лабораторной
№2 благодаря интеграции Jersey:

- `POST /booking/sell/{ticket-id}/{person-id}/{price}`;
- `DELETE /booking/person/{person-id}/cancel`.

Сборка и локальный запуск одного экземпляра:

```bash
./gradlew clean test bootJar
java -jar build/libs/booking-service-1.0.0.jar
```

Порт задаётся переменной `BOOKING_SERVICE_PORT` (по умолчанию `8082`), адрес
Ticket Service — переменной `TICKET_SERVICE_URL`. В лабораторной №4 код и API
Booking Service не меняются: переменная направляет его на REST-прослойку Mule,
например `http://127.0.0.1:8083/api/v1`.

Локальный запуск для лабораторной №4:

```bash
./scripts/run-booking-lab4-local.sh
```

Два экземпляра запускаются общим скриптом `scripts/start-cloud-helios.sh` и
балансируются HAProxy. Они используют один SQLite-файл, путь к которому
задаётся переменной `BOOKING_STORE_PATH`. Общий скрипт сохраняет его в
`.run/cloud/booking.db`.
