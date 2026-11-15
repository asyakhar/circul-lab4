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
   MULE_ARCHIVE="$(./scripts/download-mule-kernel.sh)" \
   MULE_HOME="$PWD/.run/mule-kernel-4.10.0" \
     ./scripts/install-mule-helios.sh
   MULE_HOME="$PWD/.run/mule-kernel-4.10.0" ./scripts/run-mule-local.sh
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

## Развёртывание на Helios

Deploy-скрипт без авторизации скачивает Mule Kernel 4.10.0 из публичного
официального Nexus MuleSoft, проверяет зафиксированную SHA-256, переносит
дистрибутив, запускает сервисы и выполняет smoke test:

```bash
./scripts/deploy-lab4-helios.sh
```

Архив кэшируется в `.run/deploy-cache` и в Git не попадает. При необходимости
можно передать уже скачанный совместимый ZIP вручную:

```bash
MULE_ARCHIVE=/path/to/mule-standalone-4.10.0.zip \
  ./scripts/deploy-lab4-helios.sh
```

Используемая REST-прослойка совместима с Mule Kernel и не требует
Enterprise-лицензии или учётной записи MuleSoft.

Официальный архив:
`https://repository-master.mulesoft.org/nexus/repository/public/org/mule/distributions/mule-standalone/4.10.0/mule-standalone-4.10.0.zip`.
Проверяемая SHA-256:
`18df4614ee2ffb7ac70cb50142df7741362ea623af2262e62467781b48f18cf2`.

По умолчанию используется учётная запись `s408303`, SSH-порт `2222` и каталог
`/tmp/s408303/circul-lab4`. Домашняя квота Helios слишком мала для WildFly,
Mule и трёх артефактов одновременно, поэтому рабочее развёртывание хранится в
защищённом временном каталоге и восстанавливается deploy-скриптом после очистки
`/tmp`. Значения переопределяются переменными `HELIOS_USER`,
`HELIOS_HOST`, `HELIOS_SSH_PORT`, `HELIOS_KEY` и `HELIOS_REMOTE_DIR`.
Если существует `~/.ssh/helios/main`, deploy-скрипт использует этот ключ
автоматически.

На сервере скрипт разворачивает SOAP Ticket Service в WildFly, Mule REST
adapter в standalone Mule Runtime, запускает неизменённый Booking Service с
`TICKET_SERVICE_URL`, направленным на Mule, и клиент. Затем автоматически
проверяются обе интеграционные цепочки и преобразование SOAP Fault.
На FreeBSD Mule запускается через его Java bootstrap без 32-битного Tanuki
wrapper из дистрибутива и использует установленную на Helios Java 17, которую
требует Web Service Consumer.

Серверные команды для ручной диагностики:

```bash
cd /tmp/s408303/circul-lab4
./scripts/start-lab4-helios.sh
./scripts/stop-lab4-helios.sh
```

Публичный клиент автоматически публикуется в персональном каталоге ИТМО:
`https://se.ifmo.ru/~s408303/circul/`. Страница обращается к Mule и Booking
Service через серверный PHP-прокси, поэтому не требует SSH-туннеля и не
использует `localhost` в браузере. Путь `circul` переопределяется переменной
`PUBLIC_PATH`.
