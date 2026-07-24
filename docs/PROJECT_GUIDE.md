# Sisdent project guide

## Purpose

Sisdent is an early REST API for managing patients in a dental clinic. The
current scope covers patients, addresses, states, JWT authentication, users,
roles, and permissions. Scheduling, practitioners, clinical records,
treatments, and billing are not implemented yet.

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
- Authenticate with an identification type and normalized identification
  number, issuing one-hour JWT access tokens.
- Manage users and their permissions through admin-only endpoints, with logical
  deletion.
- List countries from Europe, North America, and South America.
- Associate addresses with a country of residence.
- Associate patients with nationality and a unique normalized identification.

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/api/states` | List states |
| `GET` | `/api/countries` | List countries ordered by name |
| `GET` | `/api/addresses` | List addresses |
| `GET` | `/api/addresses/postal-code/{postalCode}` | Find address by postal code |
| `GET` | `/api/patients` | List patients |
| `GET` | `/api/patients/{id}` | Find patient by ID |
| `POST` | `/api/patients` | Create patient and missing address/state |
| `GET` | `/api/specialities` | List specialities with their procedures |
| `POST` | `/api/specialities` | Create a speciality and its procedures |
| `PUT` | `/api/specialities/{id}` | Replace a speciality and its nested procedures |
| `POST` | `/api/auth/login` | Authenticate and issue a JWT |
| `GET` | `/api/users` | List active users (admin only) |
| `GET` | `/api/users/{id}` | Find an active user (admin only) |
| `POST` | `/api/users` | Create a user (admin only) |
| `PUT` | `/api/users/{id}` | Update a user (admin only) |
| `PUT` | `/api/users/{id}/permissions` | Replace permissions (admin only) |
| `DELETE` | `/api/users/{id}` | Logically delete a user (admin only) |
| `PATCH` | `/api/users/me/password` | Change the authenticated user's password |
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
| Security | Spring Security, OAuth2 Resource Server, signed JWT, BCrypt |
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
- Identification type is required: `NATIONAL_ID` or `PASSPORT`.
- Identification number accepts letters, numbers, spaces, and hyphens. It is
  normalized to uppercase without spaces or hyphens before persistence.
- Login applies the same normalization, so identification numbers are
  case-insensitive (`admin`, `Admin`, and `ADMIN` all resolve to `ADMIN`).
- The normalized identification number is globally unique through a database
  constraint; duplicate creation returns HTTP `409 Conflict`.
- Patient nationality and address country use two-letter ISO 3166-1 codes.
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

Flyway applies the versioned SQL files in `src/main/resources/db/migration`
before Hibernate starts. Hibernate uses `ddl-auto=validate`, so model/schema
drift stops startup instead of silently changing the database. When the
database is empty, `InitialDataLoader` reads
`src/main/resources/data/initial-data.json` and inserts demonstration countries,
states, addresses, and patients. Country reference data contains 80 sovereign
states from Europe, North America (including Central America and the Caribbean),
and South America. Codes follow ISO 3166-1 alpha-2.

Country data is local so startup and patient creation do not depend on an
external service. ISO's Online Browsing Platform is the authoritative
maintenance source. REST Countries may support a future offline update tool,
but it is not a runtime dependency.

The default local in-memory database still loses API data when the process
ends. Pre-production uses file-backed H2 in a persistent Docker volume. Render
must use a persistent database to retain data between service replacements.

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

Local development creates a training administrator with identification
`NATIONAL_ID / ADMIN` and password `admin`. These deliberately weak credentials
exist only to simplify local exercises. Every deployed
environment must override `JWT_SECRET`, `BOOTSTRAP_ADMIN_IDENTIFICATION_NUMBER`,
and `BOOTSTRAP_ADMIN_PASSWORD`. The JWT secret must contain at least 32
characters.

Login example:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"identificationType":"NATIONAL_ID","identificationNumber":"ADMIN","password":"admin"}'
```

Send the returned token as `Authorization: Bearer <accessToken>`. Permission
changes and logical deletion affect newly issued tokens; an already issued
token remains valid until its one-hour expiry.

Every authenticated role can change its own password by sending the current
and new passwords to `PATCH /api/users/me/password`. The current password is
verified with BCrypt before the new BCrypt hash is stored.

Initial authorization matrix:

| Role | Non-user services | User service |
| --- | --- | --- |
| `ADMIN` | Create, update, read, delete | Full access |
| `MANAGER` | Create, update, read, delete | No access |
| `USER` | Read | No access |

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

On July 24, 2026, 35 tests passed with 97.87% line coverage. These values are a
snapshot and should be updated as the project grows.

## Suggested evolution

Recommended order:

1. **Durable persistence:** migrate to PostgreSQL and use environment-specific
   credentials. Flyway is already the schema authority; future changes must be
   introduced as new migrations.
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
