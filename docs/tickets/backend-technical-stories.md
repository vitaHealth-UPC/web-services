# Backend Technical Story Master Tickets

This file is the execution contract for backend agents. It normalizes the Product Backlog, Tata Tactical DDD and the Java course/reference structure without copying the Android package layout literally.

## Branch matrix

| TS | Branch | Owning Bounded Context | Sprint | Related behavior |
|---|---|---|---|---|
| TS-01 | feature/ts-01-pin-authentication | Identity & Subscription | 1 | US-01 |
| TS-02 | feature/ts-02-care-link-api | Care Link | 1 | US-02, US-12, US-13 |
| TS-03 | feature/ts-03-medication-treatment-api | Treatment Management | 1 | US-03, US-04, US-14..US-19 |
| TS-04 | feature/ts-04-intake-confirmation-api | Intake Execution | 2 | US-06, US-21, US-23 |
| TS-05 | feature/ts-05-unconfirmed-intake-processing | Omission & Escalation | 2 | US-22, US-23, omission flow |
| TS-06 | feature/ts-06-push-notification-integration | Omission & Escalation | 2 | reminder/alert delivery |
| TS-07 | feature/ts-07-caregiver-account-session-api | Identity & Subscription | 1 | US-10, US-11 |
| TS-08 | feature/ts-08-intake-schedule-generation | Intake Execution | 1 | US-05, US-20, US-24 |
| TS-09 | feature/ts-09-family-summary-history-api | Family Monitoring | 2 | US-25, US-26, US-27, US-29..US-31 |
| TS-10 | feature/ts-10-adherence-analytics-patterns | Adherence Analytics | 3 | US-08, US-09, US-32..US-34 |
| TS-11 | feature/ts-11-voice-recognition-integration | Intake Execution | 2 | US-06 voice flow |
| TS-12 | feature/ts-12-inventory-replenishment-api | Inventory & Replenishment | 4 | US-40..US-43 |
| TS-13 | mobile-owned; no backend branch | mobile-android / cross-product | 2 | local persistence + sync |
| TS-14 | feature/ts-14-plans-subscriptions-api | Identity & Subscription | 4 | US-44, US-45 |

TS-13 deliberately has no backend branch. The story is about local mobile persistence/synchronization. Backend operations needed by synchronization should be idempotent and well-documented, but a new backend sync endpoint requires an explicit backlog/OpenAPI decision.

---

## TS-01 — PIN authentication service

**Goal:** validate the older adult PIN and authenticate requests in a controlled way.

**Domain / Identity & Subscription**
- PIN credential and attempt/lock policy.
- Never persist or log plaintext PIN values.

**Application**
- RegisterPinCommandHandler.
- AuthenticateWithPinCommandHandler.
- Session/account query where the contract requires it.

**Interfaces**
- SessionsController.
- Request/response resources and assemblers.

**Infrastructure**
- Credential hashing adapter.
- Session/token adapter.
- Persistence of authentication state where required.

**Tests**
- correct/incorrect PIN;
- attempt threshold and temporary lock;
- locked request;
- expiry/unlock;
- no secret leakage.

---

## TS-02 — Care Link API

**Goal:** create and validate an authorized family/caregiver relationship with an older adult.

**Domain / Care Link**
- OlderAdult profile.
- CareLink aggregate.
- temporary LinkCode.
- consent state.
- CareLinkConfirmationPolicy.

**Application**
- RegisterOlderAdultProfileCommandHandler.
- GenerateLinkingCodeCommandHandler.
- AcceptCareLinkCommandHandler.
- RegisterConsentCommandHandler.
- ConfirmCareLinkCommandHandler.
- AssociateEmergencyContactCommandHandler.

**Interfaces**
- OlderAdultsController.
- CareLinksController.
- Resources and assemblers for profile, code, consent and status.

**Integration**
- Account identity is consulted through a public contract/port, never through Identity repositories.

**Tests**
- valid, invalid and expired codes;
- missing consent;
- accepted/rejected relationship;
- unauthorized access isolation.

---

## TS-03 — Medication and Treatment API

**Goal:** persist and expose treatment configuration managed by the caregiver.

**Domain / Treatment Management**
- Treatment aggregate.
- Medication entity.
- Dose/Dosage, Frequency, intake schedule, instructions and ReminderConfig.
- Treatment/Medication status.
- completeness invariant before activation.

**Application**
- CreateTreatmentCommandHandler.
- RegisterMedicationCommandHandler.
- EditMedicationCommandHandler.
- DeactivateMedicationCommandHandler.
- DefineDoseAndFrequencyCommandHandler.
- ConfigureScheduleCommandHandler.
- ConfigureRemindersCommandHandler.
- ActivateTreatmentCommandHandler.
- PauseTreatmentCommandHandler.
- GetTreatmentDetailQueryHandler.

**Interfaces**
- TreatmentsController.
- MedicationsController.
- Resources and assemblers.

**Infrastructure**
- Treatment JPA repository adapter.
- Care Link verification adapter/ACL when authorization requires it.
- In-process TreatmentActivated / treatment-update publication.

**Critical rules**
- Deactivation/pause preserve history.
- Incomplete treatment cannot activate.
- Changes affect future execution according to the domain design.

**Tests**
- aggregate completeness;
- create/edit/deactivate;
- activate/pause;
- access restriction;
- REST and persistence mapping.

---

## TS-04 — Intake confirmation API

**Goal:** register a dose/intake confirmation idempotently.

**Domain / Intake Execution**
- Intake aggregate.
- ConfirmationChannel.
- IntakeStatus.
- ToleranceWindow.

**Application**
- ConfirmIntakeCommandHandler.
- Voice recognition, after validation, must delegate to the same Intake confirmation invariant.

**Interfaces**
- IntakeConfirmationController.
- IntakeConfirmationResource.
- request/command and result/resource assemblers.

**Infrastructure**
- Intake repository.
- domain-event publication for intake history/outcome.

**Critical rule**
A retry for the same intake must not create a second business confirmation or duplicate side effects.

**Tests**
- first confirmation;
- duplicate/retry;
- late-but-valid confirmation;
- confirmation after definitive state;
- transactional/concurrent protection.

---

## TS-05 — Automatic unconfirmed-intake processing

**Goal:** evaluate unresolved intakes, identify expiry/omission, and produce the domain outcomes.

**Domain / Omission & Escalation**
- OmissionCase aggregate.
- GracePeriod.
- CareAlert.
- EscalationRecord.
- EscalationPolicy.

**Application**
- OpenOmissionCaseCommandHandler.
- SendReinforcedReminderCommandHandler.
- ResolveOmissionCaseCommandHandler.
- EvaluateGracePeriodCommandHandler.
- RegisterOmissionCommandHandler.
- GenerateCaregiverAlertCommandHandler.
- EscalateOmissionCommandHandler.
- CloseOmissionCaseCommandHandler.

**Interfaces**
- event consumers for unconfirmed/confirmed intake.
- The current Tactical DDD does not require a direct REST controller for the automatic core flow.

**Infrastructure**
- OmissionEvaluationScheduler.
- JPA repository.
- Spring event listeners/publisher.

**Tests**
- scheduler/handler idempotency;
- confirmation during grace resolves;
- expiry produces one omission;
- repeated evaluation does not duplicate alert/escalation state.

---

## TS-06 — Push notification integration

**Goal:** deliver reminders and caregiver alerts without coupling Domain to a vendor.

**Application contract**
- INotificationPort.

**Infrastructure**
- PushNotificationAdapter as Anti-Corruption Layer.
- Record provider success/failure.
- Obtain quiet-hours/channel preferences through the Accessibility & Preferences public contract.

**Rules**
- Provider DTOs stay in Infrastructure.
- Delivery failure must not erase/roll back the underlying business state.
- Provider secrets never enter source control.

**Tests**
- provider mapping;
- success/failure;
- quiet-hour/channel decision;
- retry behavior if specified.

---

## TS-07 — Caregiver account and session API

**Goal:** register, verify and authenticate the family/caregiver account.

**Domain / Identity & Subscription**
- Account aggregate.
- EmailAddress and status value objects.
- verification lifecycle.
- AccountAccessPolicy.

**Application**
- RegisterFamilyAccountCommandHandler.
- VerifyEmailCommandHandler.
- AuthenticateFamilyCommandHandler.
- session/token port.

**Interfaces**
- AccountsController.
- SessionsController.
- registration, verification and sign-in Resources/assemblers.

**Infrastructure**
- email verification adapter;
- credential hashing;
- token/session adapter;
- account repository.

**Tests**
- duplicate email;
- valid/expired verification;
- unverified/disabled account rejection;
- valid session;
- safe error semantics.

---

## TS-08 — Intake schedule generation

**Goal:** generate future scheduled intakes from an active treatment.

**Domain / Intake Execution**
- IntakeSchedulingService.
- MedicationSnapshot.
- future-intake scheduling invariants.

**Application**
- GenerateIntakeScheduleCommandHandler.
- IssueReminderCommandHandler.
- ReinforceReminderCommandHandler.
- GetNextIntakeQueryHandler.
- GetDailyIntakeAgendaQueryHandler.

**Interfaces**
- TreatmentActivated/TreatmentUpdated event consumers.
- IntakeQueriesController.

**Infrastructure**
- ReminderScheduler.
- ReminderReinforcementScheduler.
- Intake repository and Spring event listeners.

**Critical rule**
Treatment updates regenerate only future intakes without definitive outcomes; resolved history is preserved.

**Tests**
- one/multiple schedule times;
- chronological agenda;
- regeneration preserves history;
- reminder only for pending intake.

---

## TS-09 — Family summary and history API

**Goal:** provide caregiver-facing recent status, history and alert information.

**Domain / Family Monitoring**
- FamilyMonitor/OlderAdultStatus projection according to the Tactical design.
- AlertSummary.
- Caregiver follow-up note.

**Application**
- GetOlderAdultStatusQueryHandler.
- GetRecentIntakeHistoryQueryHandler.
- GetAlertDetailQueryHandler.
- GetContactChannelQueryHandler.
- CreateCaregiverNoteCommandHandler.
- MarkAlertAttendedCommandHandler.
- CloseAlertCommandHandler.
- RegisterAlertFromOmissionCommandHandler.

**Interfaces**
- FamilyMonitoringController.
- AlertsController.
- CaregiverNotesController.

**Integration**
- consumers for IntakeOmitted, LowStockDetected and AdherencePatternDetected.
- source-context information is obtained through ports/ACLs, never direct repositories.

**Tests**
- summary projection;
- no-data state;
- alert attended history;
- note author/time;
- authorization by care relationship.

---

## TS-10 — Adherence calculation and pattern detection

**Goal:** calculate adherence indicators and identify recurring patterns.

**Domain / Adherence Analytics**
- Adherence ledger/snapshots.
- AdherencePattern.
- AdherenceTolerancePolicy.
- AdherencePatternDetectionService.
- OmissionRiskEstimationService.
- AdherenceInsightGenerationService.

**Application**
- history event ingestion.
- ConsolidateWeeklyPeriodCommandHandler.
- CalculateAdherenceRateCommandHandler.
- DetectAdherencePatternCommandHandler.
- query handlers for history, metrics, patterns and recommendations.

**Infrastructure**
- repositories/projections.
- WeeklyConsolidationScheduler.
- AdherencePatternDetectionScheduler.
- Spring event listeners.

**Rules**
- Analytics consumes intake outcomes; it does not modify medication prescriptions.
- Recommendations may guide reminders/scheduling/follow-up, never dosage or medical indication.

**Tests**
- percentage boundaries;
- on-time/late/omitted input contract;
- recurrence threshold;
- insufficient evidence;
- idempotent scheduled execution.

---

## TS-11 — Voice recognition integration

**Goal:** convert a spoken confirmation into validated input for the Intake confirmation invariant.

**Application**
- IVoiceRecognitionPort.
- ConfirmIntakeByVoiceCommandHandler.

**Domain**
- VoiceConfirmationValidationService.

**Infrastructure**
- VoiceRecognitionAdapter for the provider selected by the Spike.
- provider-specific DTOs remain in Infrastructure.

**Rules**
- A transcription is not automatically a confirmation.
- Validate recognized intent/phrase before Intake.confirm.
- Provider failure leaves the intake unresolved and returns a controlled result.
- Do not persist raw audio without an explicit requirement.

**Tests**
- recognized valid phrase;
- unrecognized/ambiguous phrase;
- provider timeout/failure;
- duplicate voice request remains idempotent through TS-04 rules.

---

## TS-12 — Inventory and replenishment API

**Goal:** maintain medication stock and register replenishments.

**Domain / Inventory & Replenishment**
- Inventory aggregate.
- Batch.
- StockQuantity/StockLevel.
- reorder threshold.
- replenishment.

**Application**
- RegisterInitialInventoryCommandHandler.
- RegisterReplenishmentCommandHandler.
- ConsumeUnitCommandHandler.
- GetRemainingStockQueryHandler.
- IntakeConfirmed event handler.

**Interfaces**
- InventoryController.
- IntakeConfirmedEventConsumer.
- Inventory and replenishment Resources/assemblers.

**Infrastructure**
- Inventory JPA repository.
- event listener/publisher.

**Events**
- LowStockDetected.
- ReplenishmentRegistered.

**Tests**
- quantity cannot be negative;
- confirmed intake decrements once;
- threshold transition;
- replenishment is transactional;
- invalid request validation.

---

## TS-13 — Local storage and synchronization

**Owner:** mobile-android, not web-services.

Backend responsibilities are only contracts explicitly required by the mobile synchronization strategy:
- stable identifiers;
- idempotent mutations where required;
- deterministic resource representations;
- authorization;
- explicit conflict/error semantics.

Do not create a backend feature/ts-13 branch merely to mirror numbering. If a future backend synchronization endpoint becomes a backlog item, explicitly amend the backlog or create a new backend Technical Story.

---

## TS-14 — Plans and subscriptions API

**Goal:** manage plan catalog and account subscription/capabilities.

**Domain / Identity & Subscription**
- Plan.
- Subscription.
- PlanCapability.
- status/value objects.

**Application**
- ChangeSubscriptionCommandHandler.
- GetCurrentSubscriptionQueryHandler.
- ListAvailablePlansQueryHandler.

**Interfaces**
- SubscriptionsController.
- Plan, Subscription and ChangeSubscription Resources/assemblers.

**Rules**
- Do not hardcode capabilities in controllers or mobile UI.
- Expose capabilities as domain-backed data.
- Preserve status/renewal semantics in the domain model.

**Tests**
- current plan query;
- valid/invalid plan change;
- capability set;
- renewal/status representation;
- authorization.

---

# Definition of Done for every backend TS

1. Acceptance Criteria are traceable to tests.
2. Domain rules do not live in Controllers.
3. Commands and Queries are explicit.
4. REST Resource, domain model and persistence mapping are not collapsed into one type for convenience.
5. No direct persistence dependency across Bounded Contexts.
6. Public API changes are documented in OpenAPI.
7. Base API/documentation language is English; localizable messages support es-419.
8. Unit tests cover Domain/Application changes.
9. Repository/controller integration tests cover changed boundaries.
10. Acceptance-critical behavior has acceptance/integration coverage where appropriate.
11. mvn test passes.
12. mvn package passes.
13. No credentials/secrets are committed.
14. Branch is current with develop.
15. Pull request targets develop, not main.
