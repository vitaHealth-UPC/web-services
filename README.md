# Tata Web Services

RESTful backend for **Tata**, the VitaHealth product for medication adherence and family follow-up.

## Architecture

This repository is a **single deployable modular monolith** built with Java and Spring Boot. Its module boundaries mirror the nine Bounded Contexts defined in the Tata report:

- Identity & Subscription
- Care Link
- Treatment Management
- Intake Execution
- Omission & Escalation
- Adherence Analytics
- Family Monitoring
- Accessibility & Preferences
- Inventory & Replenishment

Each Bounded Context follows this Java structure:

~~~text
com.tata.<boundedcontext>/
├── domain/
├── application/
├── interfaces/
└── infrastructure/
~~~

The Java layout deliberately follows the course/reference backend style rather than copying the Android folders literally. Commands and Queries express CQRS intent; application handlers/services orchestrate them; REST belongs to interfaces; JPA, schedulers and provider adapters belong to infrastructure.

Read before implementing a feature:

- docs/architecture/backend-architecture.md
- docs/architecture/bounded-contexts.md
- docs/tickets/backend-technical-stories.md
- docs/tickets/agent-template.md
- docs/api/contract-guidelines.md

## Technology

- Java 26
- Spring Boot 4.1
- Spring MVC
- Spring Data JPA
- PostgreSQL
- Spring Security
- Spring Application Events for in-process domain integration
- OpenAPI / Swagger via springdoc
- Maven
- JUnit / Spring Boot Test
- Cucumber for acceptance specifications where appropriate

## Internationalization

English is the default API/documentation language. Latin American Spanish is supported through es-419.

~~~text
src/main/resources/i18n/messages.properties
src/main/resources/i18n/messages_es_419.properties
~~~

## Local configuration

The default profile is dev. Create the PostgreSQL database before starting the service.

~~~bash
export DATABASE_HOST=localhost
export DATABASE_PORT=5432
export DATABASE_NAME=tata
export DATABASE_USER=postgres
export DATABASE_PASSWORD=postgres
mvn spring-boot:run
~~~

## Deployment

This backend is a long-running Spring Boot JVM process.

**Do not deploy the API on Vercel.** Vercel is for serverless Node/edge and static sites. Use Vercel only for a landing page if needed.

Recommended student/demo layout:

1. **PostgreSQL on Railway** (managed database).
2. **API on Railway** too (simplest: same project, second service from this `Dockerfile`), **or** on **Render** Web Service with the same Docker image.
3. Point the API `DATABASE_URL` at the Railway Postgres URL.

| Variable | Notes |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | Railway Postgres URL (`postgresql://...`); mapped automatically to JDBC, including `sslmode` |
| or `DATABASE_HOST` / `DATABASE_PORT` / `DATABASE_NAME` / `DATABASE_USER` / `DATABASE_PASSWORD` | discrete alternative |
| `PORT` | provided by the host (Railway/Render) |
| `TATA_REQUIRE_AUTHENTICATION` | defaults to `true` in prod |
| `TATA_CORS_ALLOWED_ORIGINS` | comma-separated origins for mobile/web clients |

Health check: `GET /health`.

Swagger UI: `/swagger-ui.html` once the service is up.

## GitFlow

~~~text
main
  ↑
develop
  ↑
feature/ts-XX-description
~~~

Backend implementation branches are based on Technical Stories, for example:

~~~text
feature/ts-03-medication-treatment-api
feature/ts-08-intake-schedule-generation
feature/ts-10-adherence-analytics-patterns
~~~

Release and hotfix branches use:

~~~text
release/x.y.z
hotfix/x.y.z
~~~

Use Conventional Commits. Feature pull requests target develop. Release work is promoted to main through a pull request.

**TS-13 note:** TS-13 is mobile-owned local persistence/synchronization. It is documented as a cross-product dependency but intentionally has no backend feature branch unless the backlog explicitly adds a backend synchronization contract.
