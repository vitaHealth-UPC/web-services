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
