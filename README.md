# Sisdent

Spring Boot REST API using an in-memory H2 database.

## Run

```bash
./mvnw spring-boot:run
```

Then request the database-backed greeting:

```bash
curl http://localhost:8080/api/messages/hello
```

Response:

```json
{"message":"Hello World"}
```

The H2 console is available at `http://localhost:8080/h2-console` with JDBC URL
`jdbc:h2:mem:sisdent`, username `sa`, and an empty password.

## Test

```bash
./mvnw test
```
