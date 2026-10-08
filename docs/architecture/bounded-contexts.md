# Bounded Context Ownership

| Bounded Context | Owns | Does not own | Main collaboration |
|---|---|---|---|
| Identity & Subscription | accounts, authentication, PIN policy, plans, subscriptions | care relationship, treatment rules | publishes account enablement; exposes identity/capabilities |
| Care Link | older-adult profile, linking code, consent, care relationship | account credentials, treatment definition | consumes enabled account; exposes link/consent verification |
| Treatment Management | medications, dose/frequency, schedules, instructions, reminders, treatment lifecycle | execution of each scheduled intake | publishes treatment activation/update |
| Intake Execution | scheduled intakes, reminders, confirmation, tolerance, daily/next-dose agenda | definitive omission escalation, adherence analysis | consumes treatment events; publishes intake outcomes |
| Omission & Escalation | unresolved intake case, omission, caregiver alert, escalation | treatment editing, analytics | consumes intake events; uses notification preferences/provider |
| Adherence Analytics | adherence ledger/snapshots, outcome metrics, patterns, insights/recommendations | treatment prescription editing, alert delivery | consumes intake/omission outcomes |
| Family Monitoring | caregiver-facing consolidated state, alert summaries, follow-up notes | source-of-truth intake/treatment rules | consumes omission/stock/pattern events; queries through ports |
| Accessibility & Preferences | text size, contrast, reduced motion, reading/voice preferences, quiet hours, notification channels | UI rendering itself, omission business rules | exposes a public preference contract/shared concepts |
| Inventory & Replenishment | stock, batches, thresholds, replenishments | medication definition | consumes confirmed intake; publishes low-stock/replenishment |

## Integration rule

The package graph must never become a graph of repositories importing repositories. Collaboration happens through IDs, public facades/ACLs, ports, or in-process domain events.

The Android application mirrors domain boundaries as packages inside its single app module. The Java backend remains one Maven/Spring Boot deployable whose Bounded Contexts are isolated Java packages.
