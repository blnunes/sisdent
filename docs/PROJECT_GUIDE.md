# Sisdent project guide

## Purpose

Sisdent is an early REST API for managing patients in a dental clinic. The
current scope covers patients, addresses, and states. Authentication,
scheduling, practitioners, clinical records, treatments, and billing are not
implemented yet.

This document describes the system that exists today. Future ideas are kept in
a separate section so they are not mistaken for implemented features.

Related documents:

- `docs/PIPELINE.md`: tests, SonarCloud, and Render deployment.
- `docs/ARCHITECTURE.md`: components, flows, and architecture decisions.

## Current features

- List states ordered by name.
- List addresses ordered by street.
- Find an address by postal code.
- List patients ordered by name.
- Find a patient by ID.
- Create a patient with request validation.
- Reuse an existing address when its postal code already exists.
- Create a missing state and address during patient creation.
- Seed demonstration data from JSON when the database is empty.
- Expose OpenAPI documentation and Swagger UI.
- Expose an application health endpoint for the hosting platform.

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/api/states` | List states |
| `GET` | `/api/addresses` | List addresses |
| `GET` | `/api/addresses/postal-code/{postalCode}` | Find address by postal code |
| `GET` | `/api/patients` | List patients |
| `GET` | `/api/patients/{id}` | Find patient by ID |
| `POST` | `/api/patients` | Create patient and missing address/state |
| `GET` | `/actuator/health` | Application health |
| `GET` | `/v3/api-docs` | OpenAPI JSON contract |
| `GET` | `/swagger-ui.html` | Swagger UI redirect |

Published environment:

- API: `https://sisdent-yhze.onrender.com`
- Swagger UI: `https://sisdent-yhze.onrender.com/swagger-ui/index.html`

## Technology stack

| Area | Current technology |
| --- | --- |
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 |
| Web | Spring MVC |
| Persistence | Spring Data JPA and Hibernate |
| Database | In-memory H2 |
| Validation | Jakarta Bean Validation |
| API documentation | Springdoc OpenAPI 3.0.3 |
| Testing | JUnit 5, Spring Boot Test, MockMvc, and Mockito |
| Coverage | JaCoCo 0.8.15 |
| Build | Maven 3.9.x; Maven Wrapper included |
| Quality | SonarCloud |
| Container | Multi-stage Docker build with Temurin Java 25 |
| Hosting | Render free plan |
| CI/CD | GitHub Actions |

## Code structure

```text
src/main/java/br/com/itbn/sisdent/
|-- SisdentApplication.java       # application entry point
|-- config/
|   |-- InitialDataLoader.java    # JSON seed when the database is empty
|   `-- OpenApiConfiguration.java # Swagger metadata
|-- controller/                   # HTTP endpoints
|-- dto/                          # request and response contracts
|-- mapper/                       # entity-to-response mapping
|-- model/                        # JPA entities and Gender enum
|-- repository/                   # Spring Data persistence
`-- service/                      # application rules and transactions
```

The normal flow is `Controller -> Service -> Repository -> H2`. Controllers do
not access repositories directly. Request and response records are separate
from JPA entities.

`OpenApiConfiguration` is used indirectly by the Spring container. Spring Boot
finds the class through component scanning because it is annotated with
`@Configuration`; its `sisdentOpenApi` method registers an `OpenAPI` bean that
Springdoc reads when serving `/v3/api-docs` and Swagger UI. There is deliberately
no direct Java call to this class or method, so IDE "unused" inspections must not
be interpreted as evidence that the configuration can be removed.

`PatientService.create` contains the main business flow: it looks up an address
by postal code and a state by abbreviation, creates missing records, and then
persists the patient in one transaction.

Patient and address repositories use `@EntityGraph` to load required
associations and avoid extra queries while mapping response DTOs.

## Current validation rules

- Patient name is required.
- Birth date is required and must be in the past.
- `active` is required.
- Gender is required: `FEMALE`, `MALE`, or `OTHER`.
- Tax ID must contain exactly 11 digits and is unique in the database.
- Street and district are required.
- Postal code must contain exactly 8 digits and is unique.
- State name is required; abbreviation must be two uppercase letters and unique.

Validation currently checks format only. It does not validate Brazilian CPF
check digits or whether a postal code exists in the real world.

## Database and seed data

The application uses an in-memory H2 database:

```text
jdbc:h2:mem:sisdent
```

`spring.jpa.hibernate.ddl-auto=create-drop` recreates the schema for every
process. When the database is empty, `InitialDataLoader` reads
`src/main/resources/data/initial-data.json` and inserts demonstration states,
addresses, and patients.

Important consequence: data created through the API on Render disappears when
the process restarts or a new deployment occurs. The JSON seed is loaded again.
This H2 setup does not provide production persistence.

The H2 console is available locally at `/h2-console` and disabled on Render by
`H2_CONSOLE_ENABLED=false`.

## Local setup

Requirements:

- JDK 25;
- Git;
- Docker only if container testing is required;
- no system Maven installation is needed when using the wrapper.

```bash
java -version
./mvnw -version
./mvnw spring-boot:run
```

The default URL is `http://localhost:8080`. To select another port:

```bash
PORT=9090 ./mvnw spring-boot:run
```

Quick checks:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/patients
open http://localhost:8080/swagger-ui.html
```

## Test and build

```bash
# Run tests
./mvnw test

# Run the CI verification phase and generate JaCoCo output
./mvnw verify

# Build and run the JAR
./mvnw clean package
java -jar target/sisdent-0.0.1-SNAPSHOT.jar

# Build and run a container
docker build -t sisdent .
docker run --rm -p 8080:8080 -e H2_CONSOLE_ENABLED=false sisdent
```

The HTML coverage report is generated at `target/site/jacoco/index.html`.

The Docker build skips tests because the deployment pipeline verifies the code
first. Run `./mvnw verify` before building an image locally.

## Test suite

- Unit tests for services with mocked repositories.
- Unit tests for `ResponseMapper`.
- Integration tests with Spring context, H2, and MockMvc.
- Coverage of endpoints, seed data, 404 responses, invalid input, creation, and
  the OpenAPI contract.

On July 21, 2026, 20 tests passed with 97.54% line coverage. These values are a
snapshot and should be updated as the project grows.

## Suggested evolution

Recommended order:

1. **Durable persistence:** migrate to PostgreSQL, use environment-specific
   credentials, and add Flyway or Liquibase. Replace `create-drop` with
   migrations.
2. **Security and privacy:** add Spring Security, users and roles, personal-data
   protection, appropriate Tax ID masking, and audit trails.
3. **Consistent errors:** add `@RestControllerAdvice` with Problem Details,
   domain error codes, and controlled validation messages.
4. **Complete patient API:** update, deactivate, search, filter, and paginate.
5. **Dental domain:** practitioners, schedules, appointments, clinical records,
   odontograms, procedures, and attachments.
6. **Operations:** separate environments, observability, backups, alerts, and
   restoration tests.
7. **Contracts and clients:** introduce `/api/v1`, richer OpenAPI examples,
   contract tests, and restricted CORS.

Before storing real clinical data, review privacy, retention, consent, and
applicable legal requirements. Clinical data requires substantially stronger
controls than the current prototype.
