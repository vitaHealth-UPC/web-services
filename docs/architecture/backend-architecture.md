# Backend Architecture Contract

## 1. Architectural form

Tata Web Services is a **modular monolith**: one Spring Boot deployable with nine Bounded Contexts. A Bounded Context is a logical module/package boundary, not an independently deployed microservice.

Canonical names:

| Business name | Java package |
|---|---|
| Identity & Subscription | identitysubscription |
| Care Link | carelink |
| Treatment Management | treatmentmanagement |
| Intake Execution | intakeexecution |
| Omission & Escalation | omissionescalation |
| Adherence Analytics | adherenceanalytics |
| Family Monitoring | familymonitoring |
| Accessibility & Preferences | accessibilitypreferences |
| Inventory & Replenishment | inventoryreplenishment |

Package names are lowercase by Java convention. The former mixed-case placeholders and the misspellings "accesibility" / "excalation" are not canonical.

## 2. Internal structure

The backend uses the Java/Open Source Applications shape reflected in the course reference project and Tata Tactical DDD:

~~~text
com.tata.<bc>/
├── domain/
│   ├── model/
│   │   ├── aggregates/
│   │   ├── entities/
│   │   ├── valueobjects/
│   │   ├── commands/
│   │   ├── queries/
│   │   └── events/
│   ├── repositories/
│   ├── services/
│   └── factories/
│
├── application/
│   ├── commandservices/
│   ├── queryservices/
│   └── internal/
│       ├── commandservices/
│       ├── queryservices/
│       ├── eventhandlers/
│       └── outboundservices/
│
├── interfaces/
│   ├── rest/
│   │   ├── resources/
│   │   └── transform/
│   ├── events/
│   └── acl/
│
└── infrastructure/
    ├── persistence/jpa/
    ├── events/
    ├── scheduling/
    ├── security/
    └── external/
~~~

Only create subpackages that the story genuinely needs.

## 3. CQRS convention

Writes:

~~~text
HTTP request
  → Request Resource
  → Resource-to-Command assembler
  → Command
  → Application Command Handler/Service
  → Aggregate / Domain Service
  → Repository contract
  → Infrastructure repository adapter
~~~

Reads:

~~~text
HTTP request
  → Query
  → Application Query Handler/Service
  → repository/read access
  → Response Resource assembler
  → HTTP response
~~~

Do not create a second generic UseCase layer beside Commands/Queries.

## 4. Layer responsibilities

### Domain

Owns business language and invariants: Aggregates, Entities, Value Objects, Domain Services, Policies, Factories, Commands, Queries, Domain Events, repository interfaces and business-facing ports.

Domain code must not become a home for REST Resources, controller concerns, SQL/JPA query details or vendor SDK types.

### Application

Coordinates use cases. It loads aggregates, invokes domain behavior, persists through contracts, calls outbound ports, and routes domain outcomes. It should not duplicate domain rules.

### Interfaces

Owns inbound adapters: REST Controllers, request/response Resources, assemblers/transformers, event Consumers and public ACL/facade entry points.

### Infrastructure

Owns JPA implementations, schedulers, external-service adapters, token/email/push/speech providers, Spring event listeners/publishers and framework configuration.

## 5. Bounded Context isolation

A BC must not:

- import another BC's JPA repository;
- map another BC's database entity;
- query another BC's table directly;
- add cross-BC physical foreign keys.

A cross-context reference is stored as a logical identifier.

Synchronous collaboration uses a public facade/ACL/port. Event-driven collaboration in the current architecture uses Spring Application Events in the same process. There is no external message broker in the current Tata architecture.

## 6. Persistence

Tata's own architecture defines PostgreSQL as the central backend database. The database instance is physically shared, while data ownership remains logically separated by Bounded Context.

Development currently uses Hibernate schema update for convenience. Production defaults to `ddl-auto=update` for the first Railway/managed deploy (override with `DDL_AUTO=validate` once Flyway migrations exist). Hardened releases should move schema evolution to explicit migrations before locking validation.

## 7. API and i18n

- REST is the client-facing application protocol.
- OpenAPI/Swagger is the contract documentation.
- English is the default API/documentation language.
- Latin American Spanish uses es-419.
- Human-readable localized messages are not machine-readable error identifiers.
- Mobile clients consume the backend contract and must not invent routes independently.

## 8. Tests

Tests remain in this repository.

~~~text
src/test/java/com/tata/<bc>/
├── domain/
├── application/
├── infrastructure/
└── interfaces/

src/test/resources/features/
└── acceptance specifications when appropriate
~~~

A story is not complete because it compiles: tests must cover the layer and acceptance behavior it changes.

## Cross-context IDs and intake confirmation

Account, OlderAdult, CareLink, Medication, Treatment and Intake identities are UUID strings. References retain the owning context type (`String`, PostgreSQL `varchar(36)`). Internal Omission, Monitoring and Inventory IDs may remain `Long`/`BIGINT`. No physical foreign keys cross contexts.

The first confirmation persists server UTC `confirmedAt` and `confirmationChannel`, and publishes `IntakeConfirmed(String intakeId, String medicationId, String olderAdultId, Instant confirmedAt)` in the same transaction. Retries preserve that metadata and do not republish. A pessimistic row lock serializes concurrent confirmations. Omission resolution uses the event confirmation time. Status vocabulary remains `PENDING / CONFIRMED / LATE / OMITTED`; late-window classification is a separate pending policy.

Existing databases require explicit schema/data migration for numeric Omission/Monitoring references before production deployment. Demo numeric IDs cannot be mapped automatically to real UUID accounts. Hibernate schema update is not a production migration.
