# Intake agenda — TS-08 / US-24

`GET /api/v1/older-adults/{olderAdultId}/intakes/agenda?from=2026-10-05T05:00:00Z&to=2026-10-12T05:00:00Z`

Returns `200` with an array of the same resources as intake detail, including confirmation metadata and all persisted outcomes. Empty ranges return `[]`. Ordering is scheduledAt then intake ID; the range is `[from, to)`. Both parameters are required ISO-8601 instants. Reversed, equal, malformed or ranges exceeding eight elapsed days return `400`. The eight-day cap permits a seven-calendar-day week across DST changes; it does not generate extra intakes.

Android computes the local Monday start and the next local Monday start separately in the device zone, converts each to UTC, and groups returned instants back into local calendar days. Do not add a fixed 168 hours to implement a calendar week.

The existing Family Monitoring recent-history path `/older-adults/{olderAdultId}/intakes` remains separate and unchanged. This query reads Intake Execution only and does not regenerate schedules or infer omitted/late statuses from the client clock.

Schedule generation covers a seven-day horizon when a treatment is activated or updated. A daily Treatment Management scheduler republishes active schedules so Intake Execution can extend that rolling window without rewriting resolved history.

Treatment regimen configuration accepts optional `recurrenceMode`, `recurrenceZone` and `recurrenceAnchor`. Omitting all three preserves an existing regimen's recurrence; a new regimen without these fields uses UTC local-time schedules. `LOCAL_TIMES` requires a named calendar zone and no anchor; `FIXED_INTERVAL` requires a named zone and an ISO-8601 anchor at microsecond precision, together with an eight/twelve-hour frequency. The mode, zone and anchor are returned with the treatment and retained in persistence and schedule publications. Fixed intervals advance by elapsed hours from the original anchor, including across daylight-saving changes; local-time schedules follow the configured calendar zone. The seven-local-date horizon uses that zone in both modes. Reconciliation preserves matching intake identifiers and definitive outcomes.


Configured treatment frequencies use explicit local times. `DAILY`, `daily`, `Cada día` and `Diaria` repeat the supplied times each day; `Una vez al día` and `Cada 24 horas` require one time. `Cada 8 horas` requires three times separated by eight hours, including the midnight boundary; `Cada 12 horas` requires two times separated by twelve hours. `Según indicación médica` uses the explicitly supplied times. Unknown labels and incompatible time cycles are rejected without inventing additional times.

The schedule event contract interprets its clock values in UTC. Reconciliation retains matching pending intake identifiers and creation metadata, excludes definitive outcomes from changes, and cancels obsolete pending slots only inside its generated interval. Pausing a treatment removes future pending slots while preserving definitive evidence. The read-only `docs/database/treatment-preflight.sql` reports incompatible persisted frequency/time combinations before schema adoption.

Unconfirmed reporting scans reference pages of 100 ordered by scheduled time and identifier. Each candidate is locked and rechecked in an independent transaction; its report marker and synchronous omission-case handoff commit together. A failed candidate rolls back and remains eligible for a later evaluation, while the scan advances to the remaining candidates. The coordinator suspends any caller transaction. The scheduler uses `tata.intake.unconfirmed-after` (15 minutes by default) as its initial reporting delay.


Protected routes require a Bearer session token in every profile. Older-adult resources are authorized for the adult after consent or for a caregiver with a confirmed Care Link. See `sessions.md` for credential establishment and resource ownership.

Validation: H2 + MockMvc integration covers chronology, owner filtering, resolved history/metadata, inclusive start/exclusive end, empty state and invalid ranges.

Manual confirmation uses `POST /api/v1/intakes/{intakeId}/confirmation` with `{"channel":"TOUCH"}`. The manual endpoint rejects a client-claimed `VOICE` channel with `400` and leaves the intake unchanged. Spoken confirmation uses `/api/v1/intakes/{intakeId}/voice-confirmation`; the final locked transition checks the current voice preference after recognition. The HTTP flow exchanges the bearer credential for a revocable session reference before sending audio and rechecks session eligibility and consented access to the actual intake owner inside the final confirmation transaction. A session revoked or caregiver consent withdrawn during recognition produces `403` without confirming the intake. The provider call runs outside the database transaction; bearer credentials are not stored in intake commands or evidence. A definitive retry retains its original confirmation timestamp and channel, and does not publish another confirmation event.

New voice confirmations accept WAV containers with uncompressed signed PCM16 mono audio, a sample rate from 8000 to 48000 Hz, at most 60 seconds and at most 5 MiB including the container. Supported MIME types are `audio/wav`, `audio/x-wav` and `audio/wave`. The validator inspects RIFF sizes, chunk boundaries and padding, PCM format metadata and whole sample frames; a MIME label alone does not validate a recording. Unsupported, malformed or excessive recording data is rejected before transcription or a clinical transition. The servlet permits 5 MiB per uploaded file and 6 MiB for the complete multipart request.
