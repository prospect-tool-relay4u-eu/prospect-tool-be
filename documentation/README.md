# Documentation index — eu-relay-4u-prospecting-be

This folder is the detailed developer/tester documentation for the prospecting backend. The root [`README.md`](../README.md) covers setup and a quick reference; these pages go deeper into request flow, call chains and error handling so you can navigate the codebase without re-deriving it from scratch.

## What this service does

`eu-relay-4u-prospecting-be` is the business-logic backend for a schema-less prospecting tool: users own **Projects**, each with user-defined **Fields** (flexible columns), filled in as **Records** (flexible rows). It does **not** handle authentication — it only validates JWTs issued elsewhere. See [Architecture](architecture.md) for how that works.

## Tech stack

Java 21, Spring Boot 4.1.0, PostgreSQL, Spring Security (OAuth2 Resource Server), MapStruct, Springdoc/Swagger.

## Package structure

| Package | Responsibility |
|---|---|
| `controller` | REST endpoints (`ProjectsController`, `RecordsController`, `HelloController`) |
| `service` | Business logic, split into `service.project` and `service.record` (interface + `*Impl`) |
| `repository` | Spring Data JPA repositories |
| `model` | JPA entities: `Project`, `ProjectField`, `ProspectRecord`, `User`, `FieldType` |
| `dto` | Request/response records, grouped by domain (`project`, `field`, `record`) |
| `mapper` | MapStruct entity ↔ DTO mappers |
| `exception` | Custom exceptions + `GlobalExceptionHandler` |
| `security` | `UserJwtAuthenticationConverter` |
| `configuration` | `SecurityConfig`, `CorsConfig`, `MapperConfig`, `SwaggerConfig` |

## Running locally

```bash
./mvnw spring-boot:run
```

Starts on `http://localhost:8080`. Requires PostgreSQL (`eu_relay_4u_prospecting` database) and a running `relay4u-auth-service-be` (default expected at `http://localhost:8081`, see `AUTH_SERVICE_JWKS_URI`) for JWT validation to succeed.

## Contents

- [`architecture.md`](architecture.md) — security model, and how this service fits with `relay4u-auth-service-be` and the frontend
- [`endpoints/projects.md`](endpoints/projects.md) — `ProjectsController` (projects, fields, nested records)
- [`endpoints/records.md`](endpoints/records.md) — `RecordsController`
- [`exceptions.md`](exceptions.md) — exception → HTTP status mapping
