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

Package names are lowercase by Java convention and match the canonical context names.

## 2. Internal structure

The backend uses the Java/Open Source Applications shape reflected in the course reference project and Tata Tactical DDD:

~~~text
com.vitahealth.tata.<bc>/
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

Public command/query service interfaces and application exceptions define the inbound application contracts. REST controllers depend on these interfaces; their implementations and transaction boundaries belong in `application/internal`. Public context facades live in `interfaces/acl`, with delegation in `application/acl`. Application handlers coordinate through domain ports. Family Monitoring and Omission & Escalation implement source-context mapping in `infrastructure/acl`; other contexts also contain adapters in `application/internal/outboundservices/acl`.

Coordinates use cases. It loads aggregates, invokes domain behavior, persists through contracts, calls outbound ports, and routes domain outcomes. It should not duplicate domain rules.

Care Link's public command facade delegates profile registration, basic data updates, contact association, code issuance, acceptance, consent and withdrawal to individual transactional handlers. Profile changes share an ownership guard and reserve the profile row before applying domain mutations. The handlers share caregiver eligibility through an application guard and consume the configured Clock. Relationship transition and session issuance join one transaction, so a credential failure rolls back code acceptance. Immutable linking evidence and consent decisions remain in the domain.

`RegisterConsentCommandHandler` records the adult decision and delegates approval to `ConfirmCareLinkCommandHandler`, which requires the existing consent transaction and owns confirmation policy, persistence and event publication. The decision outcome identifies matching replays under the relationship lock. Replays preserve occurrence timestamps and do not publish another confirmation or issue another credential. Failures in event consumers or credential issuance roll back approval and confirmation together.

Acceptance reserves matching code evidence and consent reserves the relationship row with pessimistic database locks. Repository mutation lookups require an existing caller transaction; lifecycle writes update the managed row while preserving relationship identity, members, code and creation evidence.

Linking-code issuance coordinates its short namespace through a context-owned JPA row. Active reservations have a named unique constraint; used and expired values remain in historical relationship evidence and can be allocated again. The local SecureRandom adapter lives in `infrastructure/codegeneration`, and the application allocation port keeps reservation coordination independent of JPA.

Adult withdrawal reserves the relationship and publishes a consent-withdrawn event. Care Link records original approval and withdrawal evidence; Identity invalidates its own credentials scoped to that link. Both consumers require the command transaction, preserving atomicity across current relationship state, historical evidence and access revocation. Live session checks require the exact relationship scope to remain authorized.

### Interfaces

Owns inbound adapters: REST Controllers, request/response Resources, assemblers/transformers, event Consumers and public ACL/facade entry points.

### Infrastructure

Persistence entities contain JPA mappings; assemblers reconstruct domain objects while repository adapters keep the persistence session open for loading aggregate collections. Domain aggregates contain no JPA or Spring dependencies.

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
- Shared defines the reusable bearer scheme. Identity applies the session filter's method and path policy to every documented operation, including grouped documents. Public operations have an empty security requirement; protected operations require bearer authentication.
- English is the default API/documentation language.
- Latin American Spanish uses es-419.
- Regional English and Spanish requests fall back to their language bundles. Identity resolves public application and validation failure messages at the REST boundary; domain state and error codes remain independent of request language.
- Human-readable localized messages are not machine-readable error identifiers.
- Mobile clients consume the backend contract and must not invent routes independently.

## 8. Tests

Tests remain in this repository.

~~~text
src/test/java/com/vitahealth/tata/<bc>/
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

Unconfirmed reporting separates reference scanning from the evidence transition. The batch coordinator suspends ambient transactions and advances through schedule/id keyset pages. A dedicated handler locks each intake in a new transaction, rechecks eligibility and commits its report marker with synchronous omission-case consumers. Failed candidates roll back independently and remain available for retry; competing evaluations observe the committed marker before deciding whether to publish.

Existing databases require explicit schema/data migration for numeric Omission/Monitoring references before production deployment. Demo numeric IDs cannot be mapped automatically to real UUID accounts. Hibernate schema update is not a production migration.

## Shared responsibilities

`shared/domain/validation/DomainText` normalizes required domain text without framework dependencies. Business invariants remain inside their context.

`shared/interfaces/rest/transform/ProblemDetailAssembler` builds the common HTTP problem envelope. Each context chooses its own error code and status.

`shared/infrastructure/time/ClockConfiguration` supplies a UTC `Clock` aligned to microsecond precision before application transitions produce occurrence timestamps. This preserves equal-time transitions across database round trips without allowing domain timestamps to move backwards. Fixed clocks use the same precision adapter when testing persistence boundaries.

Shared infrastructure also owns database URL parsing, physical table naming, OpenAPI, CORS and internationalization configuration. It does not depend on business contexts.

`AuditableAbstractPersistenceEntity` supplies technical `persisted_at` and `modified_at` timestamps for identity accounts through the shared clock. Migration V17 initializes existing rows at audit introduction; these values describe persistence auditing and do not replace account verification, subscription or other domain timestamps.

The architecture checks verify package paths, independent domain code, public REST application contracts and private cross-context boundaries.

### Omission event ingress

Omission & Escalation receives Intake Execution events through the synchronous consumers in `interfaces/events`. Each consumer translates source evidence into a local command. `TrackUnconfirmedIntakeCommandHandler` reserves the pending intake, opens its case and queues the reminder in one transaction. Confirmation delegates to the resolution command with the exact recorded confirmation time.

Omission publishes the sealed `OmissionEvent` family through its application publication port. The Spring adapter preserves concrete event types and synchronous consumer failures so the caller transaction controls rollback. Device delivery uses the durable notification pipeline independently of local business event publication.

`OmissionCase` owns alert delivery transitions. `CareAlert` exposes immutable evidence, so callers cannot change the aggregate through a retained child reference. The completion processor supplies accepted provider evidence to the aggregate after durable device work finishes; repeated completion retains the original successful acceptance.

Clinical omission evaluation suspends ambient caller transactions. Its registration, missing-alert recovery and escalation commands commit independently, while each command retains its own atomic domain transition. Work scans use bounded scalar pages with strictly advancing cursors, and a failed case does not mark the caller transaction for rollback.

### Family Monitoring event ingress

Family Monitoring receives care-link, omission, analytics and inventory facts through synchronous consumers in `interfaces/events`. These consumers translate external evidence into local commands. `InitializeFamilyMonitorCommandHandler` creates relationship follow-up within the consent confirmation transaction, preserving rollback and existing history on replay. Domain decisions remain in application handlers and aggregates.

Follow-up belongs to an exact care relationship. Current caregiver queries select consented history through `FamilyMonitorSelectionService`; source projections traverse bounded identity pages and reserve each authorized follow-up. Managed persistence checks the observed revision and preserves owned child identities. A unique care-link constraint prevents duplicate follow-up for one relationship while retaining separate history for later relationships.

Human services return immutable application results. Attended alerts remain pending until explicit closure, and closed evidence remains available through alert detail. Private adult reminders use separate create/query handlers and append-only storage. Their owner comes from the authenticated adult session, independently of caregiver intervention notes. Query ACLs preserve valid absence and classify inconsistent source evidence as a server failure with a fixed public response.

Caregiver intervention notes are selected by both adult and caregiver within their relationship follow-up. The notes HTTP contract retains its existing caregiver selector and authorization checks, and carries that selector into application queries and persistence. Note creation uses the common application clock.

Alert detail and intervention commands retain the authorized caregiver when selecting relationship follow-up. Alert identifiers are resolved inside that caregiver-adult aggregate; another follow-up does not expose or modify the alert even when both caregivers are authorized for the same adult. Alert closure uses the application clock.

Summary, recent intake history and contact queries also retain the authorized caregiver when locating follow-up. Intake history, adherence totals and registered contact data remain adult-owned source information; pending alert state comes from the selected caregiver relationship.

Omission projection scans relationship follow-up identities in bounded ordered pages, refreshes each selected monitor under a write reservation and checks its exact care-link ownership and current consent through the public Care Link facade. Each eligible follow-up receives its own alert with the original omission occurrence time; repeated source events retain the existing alert and withdrawn links are skipped within the synchronous source transaction.

`AuthorizedFamilyMonitorProjectionService` shares bounded selection, row reservation and exact-link consent checks between omission and inventory projections. `InventoryStockChanged` carries the complete stock snapshot and flushed source revision after initial registration, consumption and replenishment. Family resolves medication ownership through the public Treatment contract and projects newer evidence to every currently authorized follow-up within the source transaction. Exact replays and older revisions leave follow-up unchanged; conflicting evidence at the same revision fails the transaction. Healthy source positions remain stored after notice removal, preventing obsolete shortage events from recreating a resolved notice. While stock remains low, quantity changes retain the notice identity and original detection instant.

Adherence pattern projection also uses the authorized relationship traversal. It requires the referenced medication to exist and belong to the reported adult, preserves the source period and detection time, and never substitutes a fabricated medication name when source evidence is absent.

`FamilyMonitorSelectionService` resolves participant-scoped follow-up history in bounded pages and selects exactly one relationship with current exact-link consent. It does not choose the first or newest historical match. Reads use the current snapshot; note and alert writes reserve and refresh the selected follow-up inside the command transaction. Revoked history remains stored and separate from replacement relationship data.

Family follow-up writes compare the snapshot revision with the refreshed reserved row, then update the existing managed children. Original relationship references and creation time remain unchanged; alert, note and insight history retains its stored identities. Child-only changes advance the parent revision even when the audit clock stays fixed. New note responses identify the stored child created by that command rather than selecting by note date.


## Inventory snapshot persistence

Inventory domain snapshots carry the persistence revision observed when they were loaded. New stock has no revision until its first insert. Saving existing stock reserves and refreshes the managed parent, compares the observed revision, and rejects an obsolete snapshot before applying quantity or receipt changes. A successful write flushes the new revision before returning its snapshot. This protects detached snapshots crossing transaction boundaries as well as writers sharing a persistence context. Medication identity, replenishment threshold and original creation time remain fixed. Existing receipt identifiers, quantities, registration instants and lot labels remain unchanged. The complete replacement history is validated before managed state changes; only new receipt facts are appended, and any stock increase must be supported by those receipts.

## Inventory consumption recovery

Inventory records durable consumption intent synchronously within the original Intake confirmation transaction. The interface consumer maps the original intake, medication, adult and confirmation timestamp into a local command. The Intake ACL reserves the recorded confirmation and checks every source reference before storing intent. An enqueue failure rolls back confirmation and intent together.

After source commit, an infrastructure listener accelerates processing through the recovery coordinator. Each processor uses an independent transaction and reserves source evidence, pending intent and inventory in that order. Stock decrement, the unique intake consumption marker and pending-request removal commit together. Missing or empty inventory retains the request for recovery. Replays of a completed request do not subtract another unit or recreate intent.

The scheduled recovery coordinator scans eligible requests with bounded keyset pages and a fixed cutoff. Each item has its own processing transaction; retry metadata advances in a separate transaction after failure, using increasing delays capped at one hour. Conditional retry updates preserve original source evidence and cannot recreate a completed request. The default run limit is 100 requests, with a configurable limit from 1 to 1000 and a default delay of 30 seconds. The `inventory_consumption_requests` table has an intake identity key and a due-time/identity index; references to source contexts remain logical.

## Initial family stock synchronization

New relationship follow-up stores a `FamilyStockCatchUp` request in the consent confirmation transaction. Its exclusive medication cursor and retry metadata persist in `family_stock_catchups`; replaying an existing confirmation preserves progress and does not recreate completed work. The worker discovers adult-owned medication references through bounded Treatment queries and processes one medication per independent transaction.

The source adapter reserves the medication through the public Treatment facade, then existing stock through the public Inventory facade, before reserving and refreshing the Family monitor. Initial inventory registration shares the medication reservation, covering both existing stock updates and the absence of a stock row. The worker checks actual source ownership and exact current consent, then commits the revisioned stock projection together with cursor progress. Unknown stock produces no fabricated quantity. Empty source completes work, and withdrawn consent completes it without projecting.

Scheduled recovery defaults to 30 seconds, at most 100 processing attempts per run and at most 10 steps for one relationship. `tata.family.stock-catchup-maximum-steps` accepts 1 through 1000; `tata.family.stock-catchup-delay` and `tata.family.stock-catchup-initial-delay` configure execution timing. Each failed step rolls back its projection and cursor; conditional retry updates run separately with increasing delays capped at one hour. A failed relationship or retry write does not stop later eligible relationships in the bounded scan.
