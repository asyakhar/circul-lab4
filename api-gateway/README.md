# API Gateway

The gateway intentionally uses Spring Boot 2.3 and Spring Cloud Hoxton because
that release line contains the required Zuul Proxy and Ribbon integrations.

Routes:

- `/api/v1/**` resolves `ticket-service` through Eureka and balances requests
  with Ribbon;
- `/booking/**` forwards to HAProxy at `http://localhost:8085` by default.

Run it from the repository root after Eureka and Ticket Service:

```bash
mvn -f api-gateway/pom.xml spring-boot:run
```

The HAProxy URL can be overridden with `BOOKING_LOAD_BALANCER_URL`.
