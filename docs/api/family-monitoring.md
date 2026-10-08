# Family monitoring API — TS-09

All caregiver queries and alert status updates require `caregiverId` as a query
parameter. The caregiver must have an active confirmed Care Link with the older
adult in the path; otherwise the response is HTTP 403 with
`CARE_RELATIONSHIP_REQUIRED`. A missing caregiver parameter returns HTTP 400.
Creating a note checks its existing `familiarId` author against the same care
relationship.

- `GET /api/v1/older-adults/{id}/status`: next future pending intake, latest
  definitive outcome, open alerts, and rolling seven-day `weeklyAdherence` counts.
- `GET /api/v1/older-adults/{id}/intakes?days=7`: definitive history, newest first;
  `days` accepts 1–30. Pending and future intakes are excluded.
- `GET /api/v1/older-adults/{id}/contact-channel`: the emergency phone registered
  in Care Link; absent profile/contact returns HTTP 404.
- `GET /api/v1/older-adults/{id}/alerts/{alertId}`: alert details.
- `PUT /api/v1/older-adults/{id}/alerts/{alertId}/status`: mark ATTENDED or CLOSED.
- `GET /api/v1/older-adults/{id}/notes`: notes with author and registration time.
- `POST /api/v1/older-adults/{id}/notes`: create a note with `familiarId` and `text`.

Confirming consent creates the Family Monitor in the same transaction, so a newly
confirmed link supports a summary before any omission occurs. Empty history has
zero adherence counts and no latest outcome. Analytics and Intake data cross the
module boundary through application query contracts and context-owned adapters.
