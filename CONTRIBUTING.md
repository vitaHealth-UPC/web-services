# Contributing

## Branch workflow

1. Start from an up-to-date develop branch.
2. Work only in the branch assigned to the Technical Story.
3. Keep Bounded Context ownership explicit.
4. Open the pull request against develop.
5. Do not implement feature work directly on main.

Branch naming:

~~~text
feature/ts-XX-description
fix/ts-XX-description
spike/sp-XX-description
release/x.y.z
hotfix/x.y.z
~~~

## Architectural rules

- One deployable backend, nine logical Bounded Contexts.
- No Bounded Context reads another context's JPA table/entity/repository directly.
- Cross-context synchronous collaboration goes through a public facade, ACL or port.
- Cross-context domain events use in-process Spring Application Events in the current architecture.
- Do not introduce Kafka, RabbitMQ or an additional deployable service without an approved architecture change.
- Commands and Queries are explicit CQRS intents.
- Controllers translate HTTP; they do not contain business invariants.
- Application handlers orchestrate flows.
- Aggregates, Value Objects, Policies and Domain Services own business rules.
- Infrastructure implements persistence, scheduling, external providers and Spring event plumbing.
- REST Resources are not JPA entities and are not domain aggregates.
- Cross-context references are logical identifiers; do not add physical foreign keys across BC-owned tables.
- Public code names are in English.
- English is the base API/documentation language; es-419 is the supported translation.

## Verification

Before requesting review:

~~~bash
mvn test
mvn package
~~~

A Technical Story that changes the public REST contract must update OpenAPI and its controller/integration tests. A story that changes a business invariant must add Domain/Application tests. Persistence changes require repository tests. Acceptance-critical behavior should have Cucumber or equivalent integration/acceptance coverage.
