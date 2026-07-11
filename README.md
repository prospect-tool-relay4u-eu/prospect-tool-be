# EU Relay 4U — Prospecting Backend

Backend API for a schema-less prospecting tool: users create **Projects**, define their own custom **Fields** (like flexible spreadsheet columns), and fill them in as **Records**.

## Overview

Unlike a traditional CRM with a fixed company/contact schema, this app lets each user design the shape of their own data per project. A `Project` has a user-defined list of `ProjectField`s (name, type, required, order), and each `ProspectRecord` stores its data as a flexible JSON map keyed by field key. This makes it possible to model companies, leads, contacts, or any other prospecting list without a rigid predefined structure.

## Tech stack

- **Java 21**, **Spring Boot 4.1.0**, built with the **Maven Wrapper** (`./mvnw`)
- **PostgreSQL** (Google Cloud SQL socket factory in production)
- **Spring Security** as a stateless **OAuth2 Resource Server**, validating JWTs issued by the separate [`relay4u-auth-service-be`](https://github.com/prospect-tool-relay4u-eu/relay4u-auth-service-be) via its JWKS endpoint
- **MapStruct** for entity/DTO mapping
- **Springdoc/Swagger UI** for interactive API docs
- **Docker** (multi-stage build) and **GCP Cloud Run** for deployment

## Architecture / domain model

```
User (auth principal)
 └── Project (owned by a user)
      ├── ProjectField   (key, label, type, required, order — user-defined "columns")
      └── ProspectRecord (values: Map<fieldKey, value>, stored as JSONB — the "rows")
```

- `User` — a local shadow copy (`id`, `name`, `email`) of the account managed by `relay4u-auth-service-be`, upserted from JWT claims on each authenticated request; soft-deleted. Password, lockout and email-verification state live in the auth service, not here.
- `Project` — a prospecting list/workspace owned by a `User`; soft-deleted.
- `ProjectField` — a user-defined column on a project (`type` is one of `STRING`, `BOOLEAN`, `INTEGER`, `NUMBER`); unique per `(project, key)`.
- `ProspectRecord` — one data row in a project; `values` is a JSON map keyed by `ProjectField.key`.

## Package structure

| Package | Responsibility |
|---|---|
| `controller` | REST endpoints |
| `service` | Business logic (`project`, `record`) |
| `repository` | Spring Data JPA repositories |
| `model` | JPA entities |
| `dto` | Request/response payloads (`project`, `field`, `record`) |
| `mapper` | MapStruct entity ↔ DTO mappers |
| `exception` | Custom exceptions and `GlobalExceptionHandler` |
| `security` | `UserJwtAuthenticationConverter` — turns a validated JWT into a local `User` principal |
| `configuration` | Security, CORS, MapStruct, Swagger configuration |

See [`documentation/`](documentation/README.md) for detailed endpoint-by-endpoint flow documentation.

## Getting started

**Prerequisites:** JDK 21 (no separate Maven install needed — use the bundled wrapper).

```bash
git clone git@github.com:prospect-tool-relay4u-eu/prospect-tool-be.git
cd prospect-tool-be
```

Configure the environment variables below (a local `.env` file is picked up automatically via `spring.config.import`), then run:

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. Interactive API docs are available at `http://localhost:8080/swagger-ui.html`.

## Configuration / environment variables

### Database

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL` | PostgreSQL JDBC URL | local dev default provided |
| `DB_USERNAME` | Database user | local dev default provided |
| `DB_PASSWORD` | Database password | local dev default provided |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (`dev`/`prod`) | `dev` |

### Security

| Variable | Purpose | Default |
|---|---|---|
| `AUTH_SERVICE_JWKS_URI` | JWKS endpoint of `relay4u-auth-service-be`, used to validate incoming JWTs | `http://localhost:8081/.well-known/jwks.json` |

There is no local password/JWT-issuing logic in this repo — see [`relay4u-auth-service-be`](https://github.com/prospect-tool-relay4u-eu/relay4u-auth-service-be) for registration, login, lockout and email-verification configuration.

### CORS

| Variable | Purpose | Default |
|---|---|---|
| `ALLOWED_ORIGINS` | Comma-separated list of allowed origins | `http://localhost:4200` |

## API reference

Full interactive docs (with request/response schemas) are available via Swagger UI at `/swagger-ui.html`. Summary below — for full request/response shapes, call chains and Mermaid sequence diagrams, see [`documentation/endpoints/`](documentation/README.md).

Registration/login/email-verification endpoints are **not** part of this service — see [`relay4u-auth-service-be`](https://github.com/prospect-tool-relay4u-eu/relay4u-auth-service-be) (`/api/auth/**`).

### `ProjectsController` — `/api/projects` (JWT required)

| Method | Path | Description |
|---|---|---|
| GET | `/` | List current user's projects |
| POST | `/` | Create a project |
| GET | `/{id}` | Get project detail (incl. fields) |
| PUT | `/{id}` | Update a project |
| DELETE | `/{id}` | Delete a project |
| POST | `/{id}/fields` | Add a field definition |
| DELETE | `/{id}/fields/{fieldId}` | Remove a field |
| PUT | `/{id}/fields/order` | Reorder fields |
| GET | `/{id}/records` | List records in a project |
| POST | `/{id}/records` | Create a new (empty) record |
| DELETE | `/{id}/records` | Delete all records in a project |

### `RecordsController` — `/api/records` (JWT required)

| Method | Path | Description |
|---|---|---|
| PUT | `/{recordId}` | Update a record's values |
| DELETE | `/{recordId}` | Delete a record |

### `HelloController` — `/api/hello` (JWT required)

| Method | Path | Description |
|---|---|---|
| GET | `/` | Smoke-test endpoint |

Notes: list endpoints return the full collection (no pagination). Requests are validated with Bean Validation; errors are returned as RFC-7807 `ProblemDetail` responses via a centralized `GlobalExceptionHandler` (e.g. 404 not found, 409 conflict, 400 validation, 401 bad credentials, 423 locked/blocked, 429 rate-limited).

## Authentication & security

This service is a stateless **OAuth2 Resource Server** — it does not issue tokens or manage passwords itself:

- **JWT validation**: incoming requests must carry `Authorization: Bearer <token>`; the JWT is validated against the RS256 public key published by `relay4u-auth-service-be` at `AUTH_SERVICE_JWKS_URI`. No sessions, CSRF disabled.
- **Principal resolution**: `UserJwtAuthenticationConverter` reads the `sub` (numeric user id), `email` and `name` claims from the validated JWT and upserts a local shadow `User` row, which becomes the `@AuthenticationPrincipal User` available in controllers.
- **Authorization model**: authenticated vs. unauthenticated only — no role/permission system (the converter grants no authorities).
- **Public endpoints**: `/error`, `/swagger-ui/**`, `/v3/api-docs/**`. All other endpoints require a valid JWT.

Registration, login, password hashing, account lockout and email verification are entirely owned by `relay4u-auth-service-be`. See [`documentation/architecture.md`](documentation/architecture.md) for the full cross-service flow.

## Running tests

```bash
./mvnw test          # unit/integration tests
./mvnw clean verify  # full build, same as CI
```

## Docker

```bash
docker build -t prospect-tool-be .
docker run -p 8080:8080 --env-file .env prospect-tool-be
```

The image is built as a multi-stage build (`maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre-alpine`) and exposes port `8080`.

## Branching model

- **`develop`** — default branch, where all contributor PRs land
- **`main`** — stable/release branch; a `develop` → `main` PR is opened to cut a release
- Both branches are protected: a pull request and a passing CI check are required before merging

## Deployment

GitHub Actions drive CI/CD:
- `ci.yml` — runs `./mvnw clean verify` on every pull request to `develop` or `main`
- `deploy-staging.yml` — deploys to GCP Cloud Run on push to `main`
- `deploy-prod.yml` — deploys to GCP Cloud Run on version tag push (`v*.*.*`)
- `publish-docker.yml` — publishes a public image to `ghcr.io/prospect-tool-relay4u-eu/relay4u-be` on version tag push, for the [pentest sandbox](https://github.com/prospect-tool-relay4u-eu/prospect-tool-docker)

## Docker sandbox for security testing

A ready-to-run, self-contained Docker stack (Postgres + backend + frontend) for pentesters and security researchers is available at [`prospect-tool-docker`](https://github.com/prospect-tool-relay4u-eu/prospect-tool-docker) — no local build required, `docker compose up` and go. Findings can be reported as issues on that repo.

This backend also ships a `sandbox` Spring profile (`SPRING_PROFILES_ACTIVE=sandbox`, see `application-sandbox.properties`) for use alongside the sandboxed `relay4u-auth-service-be` (which is the one that logs verification codes instead of emailing them).

## Contributing

This project is open source and welcomes contributions. See [CONTRIBUTING.md](CONTRIBUTING.md) for branching, commit style, and PR guidelines.

## License

This project is licensed under the [MIT License](LICENSE).
