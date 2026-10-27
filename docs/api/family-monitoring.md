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

## Personal routine notes

`GET /api/v1/me/notes` and `POST /api/v1/me/notes` belong to the older adult PIN session. The server derives the owner from the authenticated principal. Caregiver and link-setup sessions receive 403; absent sessions receive 401.

Create body: `title` (required, max 100), `text` (required, max 1000), `category` (`MEDICATION` or `ROUTINE`). The 201 response contains `id`, `title`, `text`, `category`, `recordedAt`. Listing is ordered by server registration time and identifier descending. Personal notes remain private and separate from caregiver intervention notes.

## Follow-up priority

Every alert in `GET /status`, `GET /alerts/{alertId}` and the `PUT /alerts/{alertId}/status` receipt includes `severity`:

| Value | Recorded omission evidence |
| --- | --- |
| `STANDARD` | An existing omission case without recorded escalation (level 0). |
| `HIGH` | Highest recorded escalation level 1 or 2. |
| `CRITICAL` | Highest recorded escalation level 3, the supported maximum. |
| `UNKNOWN` | No omission-case evidence exists for this adult and intake reference. |

This is operational follow-up priority, not a medical diagnosis or a drug-risk assessment. It is read from persisted escalation records, not inferred from a device clock, medication name or free-text reason. A newly recorded escalation appears on the next read without rewriting the Family Monitoring alert. A closed alert retains its recorded priority as historical evidence; the status endpoint excludes closed alerts from its pending collection. Attended alerts remain in that collection until explicit closure.

The caregiver relationship is authorized before priority is queried. Family Monitoring uses its own `AlertSeverityPort` and an ACL adapter to the published `OmissionContextFacade`; it does not read another context's persistence entities. The source query scopes both adult and intake references, reads scalar levels in batches of at most 100, and does not create missing cases. Malformed source evidence produces a monitoring-source failure, never an assumed standard priority. No new database column or migration is needed.

Clients may show "No critical alerts" only when all unclosed alerts have known classification and none is critical. Missing `severity` on older deployments must be treated as unknown, not as standard. An empty successfully read collection has no critical follow-ups. A failed request is not an empty collection. Classification does not change confirmation, escalation scheduling, notification delivery or caregiver status transitions.
