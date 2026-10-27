# Technical story implementation map

Tata Web Services is a Spring Boot modular monolith. This document maps the project's technical stories (TS-01–TS-14) to the actual backend responsibilities, application contracts and bounded contexts. The map describes business responsibilities and the implemented integration boundaries.

The canonical architecture is defined in [Backend Architecture](backend-architecture.md) and [Bounded Context Ownership](bounded-contexts.md). Public REST contracts and operation details are maintained in [`docs/api/`](../api/contract-guidelines.md).

## Story-to-context map

| TS | Capability | Owning bounded context | Related user stories |
| --- | --- | --- | --- |
| TS-01 | PIN access | Identity & Subscription | US-01 |
| TS-02 | Care relationships | Care Link | US-02, US-12, US-13 |
| TS-03 | Medication and treatment | Treatment Management | US-03, US-04, US-14–US-19 |
| TS-04 | Dose confirmation | Intake Execution | US-06, US-21, US-23 |
| TS-05 | Unconfirmed intake handling | Omission & Escalation | US-22, US-23 |
| TS-06 | Notification delivery | Omission & Escalation | Reminder and alert delivery |
| TS-07 | Caregiver identity | Identity & Subscription | US-10, US-11 |
| TS-08 | Intake schedule | Treatment Management / Intake Execution | US-05, US-20, US-24 |
| TS-09 | Family monitoring | Family Monitoring | US-25–US-27, US-29–US-31 |
| TS-10 | Adherence analysis | Adherence Analytics | US-08, US-09, US-32–US-34 |
| TS-11 | Spoken confirmation | Intake Execution | US-06 (voice) |
| TS-12 | Medication stock | Inventory & Replenishment | US-40–US-43 |
| TS-13 | Offline persistence and sync | Android-owned cross-product capability | Local persistence and synchronization |
| TS-14 | Plans and subscriptions | Identity & Subscription | US-44, US-45 |

## TS-01 — PIN authentication

The Identity & Subscription context establishes adult access through `PIN credentials` and `PIN sessions`. Domain policies validate credentials and control failed attempts; the security and persistence adapters implement storage and session issuance. Application handlers coordinate registration and authentication without exposing plaintext PIN values.

The HTTP boundary returns an authenticated session rather than direct access to another context's persistence. Caregiver/account sessions and adult PIN sessions remain distinct authorization scopes.

See [Sessions](../api/sessions.md) and [Session security](../api/session-security.md).

## TS-02 — Care Link

Care Link owns the older-adult profile, linking code, care relationship and consent state. Registration, code creation, link acceptance and consent transitions are handled through the context's public application layer. Other contexts request authorized link status through explicit contracts, not Care Link repositories.

A verified caregiver identity does not by itself authorize access to a specific adult's records. Active relationship and consent are required for protected operations. See [Care links](../api/care-links.md).

## TS-03 — Medication and treatment management

Treatment Management owns medication registration and catalog data, treatment regimens, frequency and time configuration, and treatment lifecycle transitions such as activation and pause. The configured treatment produces the schedule information consumed by Intake Execution, without placing actual dose-confirmation rules inside the treatment aggregate.

Regimen configuration distinguishes lead reminders and post-dose follow-up delay. The treatment and intake contexts coordinate through application contracts and schedule publications rather than shared persistence. See [Intake agenda](../api/intake-agenda.md) and [Backend architecture](backend-architecture.md).

## TS-04 — Intake confirmation

Intake Execution owns the scheduled intake and records touch/voice confirmation outcomes. The HTTP confirmation operation identifies the original intake, serializes its authoritative transition and preserves the original confirmation timestamp/channel on an idempotent replay.

The state vocabulary is `PENDING`, `CONFIRMED`, `LATE` and `OMITTED`. A manual offline request with a validated explicit occurrence time can reconcile an omitted intake while closing and retaining its omission history; requests without occurrence evidence keep the omission rejection. Confirmation metadata and the related domain event are coordinated with the persistence transaction, and client-side retries do not create additional clinical confirmations.

See [Intake agenda and confirmations](../api/intake-agenda.md) and [Voice confirmation](../api/voice-confirmation.md).

## TS-05 — Unconfirmed intake processing

Omission & Escalation owns unresolved-intake cases, omission and caregiver escalation rules. Intake Execution reports eligible unconfirmed intakes through its application/event boundary. Evaluation rechecks the source state and commits the source marker together with the corresponding consumer handoff.

This separation prevents a scheduler from equating elapsed device time with a definitive omission. Reprocessing preserves idempotency and does not manufacture duplicate cases for the same recorded source. See [Intake agenda](../api/intake-agenda.md) and [Backend architecture](backend-architecture.md).

## TS-06 — Notification delivery

Omission & Escalation coordinates reminders and caregiver alerts using notification preferences, delivery policy, persistence and an external push provider port. Device registration binds a token and installation to an authenticated owner; delivery adapters are infrastructure components rather than domain dependencies.

Notification dispatch and acknowledgment are distinct from the clinical intake status. Message identity, retry policy and recipient authorization are preserved across adapters. See [Push devices](../api/push-devices.md), [Push notification payload](../api/push-notification-payload.md) and [Notification preferences](../api/notification-preferences.md).

## TS-07 — Caregiver account and session

Identity & Subscription handles caregiver registration, email verification, account activation, password sessions and session revocation. Recovery requests are kept separate from authenticated access, and a completed credential change invalidates the relevant prior sessions.

Social sign-in exchanges verified external identity evidence for Tata authorization. Linking a social identity to an existing account requires authenticated ownership. See [Sessions](../api/sessions.md), [Verification email](../api/verification-email.md) and [Session security](../api/session-security.md).

## TS-08 — Intake schedule and agenda

Treatment Management provides the effective regimen; Intake Execution manages the generated intake schedule and read APIs. Schedule reconciliation retains matching intake identities and definitive history while updating eligible future occurrences.

The daily/weekly agenda uses a half-open interval, chronological ordering and the stored backend outcome, with next-dose queries scoped to the authorized older adult. Recurrence can be expressed using the contract's configured local time or fixed-interval settings. See [Intake agenda](../api/intake-agenda.md).

## TS-09 — Family summary, history and notes

Family Monitoring presents caregiver-facing status, recent dose information, open alerts, alert detail/status, contact information and follow-up notes. Its read layer composes authorized cross-context information through public contracts, rather than taking direct ownership of Intake, Treatment or Inventory data.

Personal adult notes and caregiver follow-up notes are distinct protected resources. Alert transitions and contact access are checked against the active caregiver relationship and consent. See [Family monitoring](../api/family-monitoring.md) and [Care links](../api/care-links.md).

## TS-10 — Adherence metrics and patterns

Adherence Analytics transforms authoritative intake/omission evidence into period summaries, per-treatment metrics, trends and patterns. Its domain types represent counts, snapshots, time bands and insights; application queries return read models suitable for API clients.

Analytical calculations do not modify treatment prescriptions, intake confirmations or escalation records. Time-zone and date-range parameters determine the period being analyzed. See [Adherence](../api/adherence.md).

## TS-11 — Voice recognition

Intake Execution receives a multipart voice recording and validates the incoming audio container before invoking the speech provider. The voice pathway uses the same authorized clinical confirmation invariant as touch confirmation, but only after a successful recognition/validation result.

The endpoint accepts supported WAV/PCM formats within the contract's bounds. Recognition, authorization and clinical persistence remain separate responsibilities; recognition failure does not record a dose as confirmed. See [Voice confirmation](../api/voice-confirmation.md).

## TS-12 — Inventory and replenishment

Inventory & Replenishment owns medication stock, remaining units, replenishment thresholds and batch records. Initial stock, retrieval and replenishment are exposed through the inventory API. Confirmed intake consumption is coordinated through cross-context application/event boundaries.

The inventory context does not redefine medication or treatment identities; it maintains its own stock ledger using references to the owning medication. See [Inventory](../api/inventory.md).

## TS-13 — Mobile offline storage and synchronization

This story is owned by the Android application. Mobile uses Room, DataStore and WorkManager to cache scheduled doses and queue touch-confirmation intents for replay with the authenticated session.

The backend provides the authoritative and idempotent intake confirmation operation; TS-13 does not imply a separate server synchronization endpoint. See [Intake agenda](../api/intake-agenda.md) and the mobile repository's offline synchronization architecture.

## TS-14 — Plans and subscriptions

Identity & Subscription owns plan catalog and account subscription operations, including capabilities and subscription changes. The client reads the available plan resources and operates on the authenticated account through the public contract.

Subscription data is not a payment transaction or a substitute for server authorization. Account and plan validation remain in the owning context. See [Sessions](../api/sessions.md) and [Backend architecture](backend-architecture.md).

## Cross-context convention

Each technical story is realized within its owning context's Domain, Application, Interfaces and Infrastructure layers. Internal integrations use public ports/ACLs or domain events. The API's documented errors and authorization checks remain authoritative for mobile clients.

This map describes responsibilities and observable contracts, while the detailed protocol and persistence rules live alongside their respective [API](../api/contract-guidelines.md) and [architecture](backend-architecture.md) documents.
