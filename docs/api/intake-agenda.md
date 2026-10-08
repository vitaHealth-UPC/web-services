# Intake agenda — TS-08 / US-24

`GET /api/v1/older-adults/{olderAdultId}/intakes/agenda?from=2026-10-05T05:00:00Z&to=2026-10-12T05:00:00Z`

Returns `200` with an array of the same resources as intake detail, including confirmation metadata and all persisted outcomes. Empty ranges return `[]`. Ordering is scheduledAt then intake ID; the range is `[from, to)`. Both parameters are required ISO-8601 instants. Reversed, equal, malformed or ranges exceeding eight elapsed days return `400`. The eight-day cap permits a seven-calendar-day week across DST changes; it does not generate extra intakes.

Android computes the local Monday start and the next local Monday start separately in the device zone, converts each to UTC, and groups returned instants back into local calendar days. Do not add a fixed 168 hours to implement a calendar week.

The existing Family Monitoring recent-history path `/older-adults/{olderAdultId}/intakes` remains separate and unchanged. This query reads Intake Execution only and does not regenerate schedules or infer omitted/late statuses from the client clock.

Schedule generation covers a seven-day horizon when a treatment is activated or updated. A daily Treatment Management scheduler republishes active schedules so Intake Execution can extend that rolling window without rewriting resolved history.

Protected routes require a Bearer session token in every profile. Older-adult resources are authorized for the adult after consent or for a caregiver with a confirmed Care Link. See `sessions.md` for credential establishment and resource ownership.

Validation: H2 + MockMvc integration covers chronology, owner filtering, resolved history/metadata, inclusive start/exclusive end, empty state and invalid ranges.
