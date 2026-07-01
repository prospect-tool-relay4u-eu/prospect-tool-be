# Contributing

Thanks for contributing to `prospect-tool-be`, the backend for the EU Relay 4U prospecting tool.

## Prerequisites

- JDK 21
- No local Maven install needed — use the bundled wrapper (`./mvnw` / `mvnw.cmd`)

## Getting started

```bash
git clone git@github.com:prospect-tool-relay4u-eu/prospect-tool-be.git
cd prospect-tool-be
./mvnw spring-boot:run
```

Local configuration lives in `.env` and `src/main/resources/application.properties`. Never commit real secrets — use placeholder values in any example config you add.

## Running tests

```bash
./mvnw test          # unit/integration tests
./mvnw clean verify  # full build, same as CI
```

## Branching & pull requests

- Branch off `main`
- Open a PR targeting `main`
- CI (`.github/workflows/ci.yml`) runs `./mvnw clean verify` on every PR and must pass before merging

## Commit messages

Keep commits short and imperative, prefixed with the type of change, e.g.:

```
fix: correct null check in ProjectServiceImpl
feat: add email verification endpoint
change: rename field to match DTO
```

## Code structure

Code is organized by layer under `eu.relay4u.prospecting`:

- `controller` — REST endpoints
- `service` — business logic
- `repository` — Spring Data JPA repositories
- `model` — JPA entities
- `dto` — request/response payloads
- `mapper` — entity/DTO mapping (MapStruct)
- `exception` — custom exceptions and handlers
- `security` — auth/JWT configuration
- `configuration` — Spring configuration classes

Place new classes in the layer matching their responsibility.

## Docker

A `Dockerfile` is provided for containerized builds (multi-stage, JDK 21, exposes port 8080):

```bash
docker build -t prospect-tool-be .
docker run -p 8080:8080 prospect-tool-be
```

## License

This project is licensed under the [MIT License](LICENSE).
