# Adherence read API — TS-10

`GET /api/v1/older-adults/{olderAdultId}/adherence/weekly?from=...&to=...`

Counts confirmed and late intakes against definitive persisted outcomes (confirmed, late and omitted). Pending outcomes do not enter the denominator. Empty evidence returns zero counts and percentage. Time bounds are inclusive start and exclusive end, ordered and limited to eight elapsed days.

`confirmedIntakes` includes both timely and late confirmations. The additive
`onTimeIntakes`, `lateIntakes` and `omittedIntakes` fields provide the outcome
breakdown without changing that existing count. Their sum equals `totalIntakes`.

`GET /api/v1/older-adults/{olderAdultId}/adherence/patterns?from=...&to=...&zone=America/Bogota`

Returns medication IDs with omissions on at least three distinct calendar days within the requested bounds. Multiple omissions on the same day count once. The optional IANA calendar zone defaults to UTC. Invalid zones or ranges return 400. Results are ordered by medication ID and contain omissionDays, firstDay and lastDay. This query reads persisted outcomes and does not infer omissions from pending intakes or change medication prescriptions.

The server configures the recurrence threshold with
`adherence.pattern.minimum-omission-days` (default 3, allowed 2–8).
# Outcome history

`GET /api/v1/older-adults/{olderAdultId}/adherence/history?from=...&to=...`
returns the definitive outcomes used for the weekly calculation. Pending intakes
are excluded. The same owner and half-open period boundaries apply to all queries.

## Follow-up recommendations

`GET /api/v1/older-adults/{olderAdultId}/adherence/recommendations?from=...&to=...&zone=UTC`
returns `REVIEW_REMINDER_AND_CAREGIVER_FOLLOW_UP` for each medication with recurring
omissions, together with the medication ID and evidence-day count. Insufficient
evidence returns an empty list. This action code invites reminder review and
caregiver follow-up; it never suggests changing a dose or medical indication.

## Persisted period evidence

`POST /api/v1/older-adults/{olderAdultId}/adherence/consolidations?from=...&to=...&zone=UTC`
captures metrics and recurrence evidence from a single outcome read. The snapshot
contains the configured recurrence threshold and consolidation time. The same
owner, bounds and normalized zone return the original persisted snapshot with
HTTP 200; retries and concurrent requests neither overwrite nor duplicate it.
Later intake changes remain visible in live queries without changing saved evidence.

`GET /api/v1/older-adults/{olderAdultId}/adherence/consolidations/{snapshotId}`
reads the saved evidence. Unknown snapshots or snapshots belonging to another
older adult return HTTP 404. Invalid bounds/zones return HTTP 400.

# Screen views for the family app

These two read endpoints serve the adherence screens of the mobile app (US-08, US-09,
US-32, US-33 and US-34). They return data and stable codes; the app owns the translated
display text. Both accept `days` (1–31, default 30) and `zone` (IANA calendar zone,
default `America/Lima`). The period ends now and starts `days` days earlier. Pending
intakes never enter any figure. Invalid values return HTTP 400 (`VALIDATION_ERROR`).

## History summary

`GET /api/v1/older-adults/{olderAdultId}/adherence/summary?days=30&zone=America/Lima`

Returns HTTP 204 when the period has no definitive intakes ("insufficient data").
Otherwise HTTP 200:

~~~json
{
  "periodDays": 30,
  "scheduledCount": 120,
  "adherencePercent": 92,
  "adherenceChangePercent": 8,
  "onTimePercent": 86,
  "onTimeChangePercent": 12,
  "lateCount": 6,
  "omittedCount": 4,
  "trend": [{ "date": "2026-09-20", "adherencePercent": 57 }],
  "recentIntakes": [
    { "scheduledAt": "2026-10-06T13:00:00Z", "medicationName": "Losartán", "status": "CONFIRMED", "minutesLate": null },
    { "scheduledAt": "2026-10-05T01:00:00Z", "medicationName": "Amlodipino", "status": "LATE", "minutesLate": 24 }
  ],
  "pattern": { "timeBand": "EVENING", "omittedCount": 4, "lateCount": 6 }
}
~~~

- `adherencePercent` counts confirmed and late intakes; `onTimePercent` counts only on-time ones.
- `adherenceChangePercent` and `onTimeChangePercent` compare with the previous period of the same
  length and are `null` when that period has no evidence.
- `trend` has up to seven points, one per equal slice of the period that has evidence.
- `recentIntakes` are the three newest. `status` is `CONFIRMED` (on time), `LATE` or `OMITTED`;
  `minutesLate` only applies to `LATE`. The medication name is the one registered when the intake was scheduled.
- `pattern` is `null` unless at least three late or omitted intakes exist and half or more of them fall in
  one time band. `timeBand` is `MORNING` (05–12), `AFTERNOON` (12–18), `EVENING` (18–22) or `NIGHT`, in the requested zone.

## Recommendations

`GET /api/v1/older-adults/{olderAdultId}/adherence/insight?days=30&zone=America/Lima`

Returns HTTP 204 when the evidence is insufficient for a conclusive recommendation. Otherwise HTTP 200:

~~~json
{
  "periodDays": 30,
  "pattern": { "type": "OMISSION", "timeBand": "EVENING", "omittedCount": 4, "lateCount": 2, "fromHour": 18, "toHour": 21 },
  "concentration": [[0.27, 0.495, 0.72, 0.42, 0.345, 0.795, 0.645], [], []],
  "recommendations": ["ADJUST_REMINDER", "REVIEW_SCHEDULE", "FOLLOW_UP_ONE_WEEK"]
}
~~~

- `concentration` has three rows (morning, afternoon, evening and night) by seven columns (Monday to Sunday),
  each cell an intensity between 0.2 and 0.8 proportional to the late or omitted intakes in that slot.
- `type` is `OMISSION` when omissions are at least as many as late intakes in the band, otherwise `LATENESS`.
- `fromHour` is inclusive and `toHour` exclusive, in local time.
- Recommendation codes only concern reminders, schedules and caregiver follow-up. They never change a
  dose or a medical indication.
