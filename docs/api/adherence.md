# Adherence read API — TS-10

`GET /api/v1/older-adults/{olderAdultId}/adherence/weekly?from=...&to=...`

Counts confirmed and late intakes against definitive persisted outcomes (confirmed, late and omitted). Pending outcomes do not enter the denominator. Empty evidence returns zero counts and percentage. Time bounds are inclusive start and exclusive end, ordered and limited to eight elapsed days.

`GET /api/v1/older-adults/{olderAdultId}/adherence/patterns?from=...&to=...&zone=America/Bogota`

Returns medication IDs with omissions on at least three distinct calendar days within the requested bounds. Multiple omissions on the same day count once. The optional IANA calendar zone defaults to UTC. Invalid zones or ranges return 400. Results are ordered by medication ID and contain omissionDays, firstDay and lastDay. This query reads persisted outcomes and does not infer omissions from pending intakes or change medication prescriptions.
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
