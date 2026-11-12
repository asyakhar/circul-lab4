# Config Server

Spring Cloud Config Server reads service settings from the root `config-repo`
directory by default.

Run it from the repository root:

```bash
mvn -f config-server/pom.xml spring-boot:run
```

Check the Ticket Service configuration:

```bash
curl http://localhost:8888/ticket-service/default
```

Set `CONFIG_REPOSITORY` to another `file:` URI when the configuration repository
is stored elsewhere.
