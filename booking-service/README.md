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
Ticket Service — переменной `TICKET_SERVICE_URL`. В лабораторной №3 этот адрес
должен указывать на Zuul Gateway, например `http://localhost:8080/api/v1`.

Два экземпляра запускаются общим скриптом `scripts/start-cloud-helios.sh` и
балансируются HAProxy. Они используют один SQLite-файл, путь к которому
задаётся переменной `BOOKING_STORE_PATH`. Общий скрипт сохраняет его в
`.run/cloud/booking.db`.
