# Web Services Agent Ticket Template

Copy this contract into every backend implementation task.

~~~text
REPOSITORY
vitaHealth-UPC/web-services

BASE
develop

WORK BRANCH
feature/ts-XX-...

SOURCE OF TRUTH
1. Product Backlog: Technical Story + related User Stories + Acceptance Criteria
2. Tata Tactical-Level DDD for the owning Bounded Context
3. Tata C4 / Context Map / Domain Message Flows
4. Current OpenAPI contract
5. Current repository architecture
6. Course/reference Java DDD project

ARCHITECTURE
Single deployable modular monolith.
Bounded Context = Java package/module boundary.

LAYERS
domain/
application/
interfaces/
infrastructure/

CQRS
Write = Command + application Command Handler/Service.
Read = Query + application Query Handler/Service.
Do not create parallel generic UseCase classes.

DEPENDENCY RULE
Do not access another BC's JPA repository/entity/table.
Cross-BC sync calls use a facade/ACL/port.
Cross-BC domain events use in-process Spring Application Events.

REST
Controller contains no business rules.
Request/response Resources are not domain models.
Assemblers translate at the interface boundary.
Update OpenAPI with public contract changes.

DATA
PostgreSQL is shared infrastructure, but tables have a logical BC owner.
No physical FK across BC-owned tables.

I18N
English is the default API/documentation language.
es-419 is the supported translation.
Localized prose is never the machine contract.

TESTS
Domain invariant tests.
Application handler tests.
Repository/integration tests when persistence changes.
Controller/API tests when the public contract changes.
Acceptance tests for acceptance-critical flows.

DONE
Acceptance Criteria traceable.
mvn test passes.
mvn package passes.
OpenAPI updated.
No cross-BC persistence coupling.
No secrets committed.
PR targets develop.
~~~
