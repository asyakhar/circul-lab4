# Ticket Service

Вызываемый сервис управления билетами. Контракт `/api/v1/**`, модели запросов
и ответов сохранены от лабораторной работы №2.

При запуске сервис:

1. запрашивает конфигурацию у Spring Cloud Config Server;
2. регистрируется в Eureka под именем `ticket-service`;
3. принимает запросы, которые Zuul направляет через Ribbon.

Для полного локального запуска сначала запустите Config Server и Eureka из
корня репозитория:

```bash
mvn -f config-server/pom.xml spring-boot:run
mvn -f eureka-server/pom.xml spring-boot:run
mvn -f ticket-service/pom.xml spring-boot:run
```

По умолчанию сервис доступен напрямую по адресу
`http://localhost:8081/api/v1`, однако клиентские запросы должны идти через
Gateway: `http://localhost:8080/api/v1`.

Для автономного диагностического запуска без Config Server импорт сделан
опциональным. Порт и context path при штатном запуске находятся в
`config-repo/ticket-service.yml`.
