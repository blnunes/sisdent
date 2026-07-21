# Sisdent

REST API built with Java 25, Spring Boot 4 and an in-memory H2 database.

## Run

```bash
mvn spring-boot:run
```

Static development data is loaded from
`src/main/resources/data/initial-data.json` whenever the application starts with
an empty database.

## Endpoints

```text
GET /api/states
GET /api/addresses
GET /api/addresses/postal-code/{postalCode}
GET /api/patients
GET /api/patients/{id}
POST /api/patients
```

Example:

```bash
curl http://localhost:8080/api/patients
```

The H2 console is available at `http://localhost:8080/h2-console` with JDBC URL
`jdbc:h2:mem:sisdent`, username `sa`, and an empty password.

## Test

```bash
mvn test
```
