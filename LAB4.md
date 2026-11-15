# Лабораторная работа №4

Локальная часть интеграции построена так:

```text
Client --------> Mule REST adapter --------> SOAP Ticket Service (WildFly)
                       ^
                       |
Booking Service -------+
```

Booking Service сохраняет API, REST-протокол и прежний сервер приложений. Его
код не адаптируется под SOAP: при запуске переменная `TICKET_SERVICE_URL`
указывает на Mule. Клиент аналогично получает адрес Mule через свою переменную
`TICKET_SERVICE_URL`.

## Сборка

```bash
./scripts/build-lab4.sh
```

Скрипт запускает тесты SOAP Ticket Service и Booking Service, после чего
собирает Mule application.

## Локальный запуск

1. Подготовить WildFly и развернуть SOAP Ticket Service существующими
   скриптами:

   ```bash
   ./scripts/setup-wildfly-local.sh
   ./scripts/deploy-ticket-soap-local.sh
   ./scripts/run-ticket-soap-local.sh
   ```

2. Указать каталог установленного Mule Runtime и в отдельных терминалах
   запустить Mule, неизменённый Booking Service и клиент:

   ```bash
   MULE_HOME=/path/to/mule-enterprise-standalone ./scripts/run-mule-local.sh
   ./scripts/run-booking-lab4-local.sh
   ./scripts/run-client-lab4-local.sh
   ```

3. Проверить обе требуемые цепочки и преобразование SOAP Fault:

   ```bash
   ./scripts/verify-lab4-local.sh
   ```

По умолчанию используются WildFly HTTP `8180`, Mule `8083`, Booking Service
`8082` и клиент `5000`. Все адреса переопределяются переменными, перечисленными
в соответствующих скриптах.

## Что остаётся выполнить на Helios

- установить Mule Runtime;
- перенести и развернуть собранные WAR/JAR;
- указать реальные Helios-порты в свойствах Mule и переменных сервисов;
- выполнить тот же интеграционный smoke test через SSH-туннели.
