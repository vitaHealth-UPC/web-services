# Notification preferences

`GET /api/v1/users/{userId}/preferences` returns the quiet interval and `notificationChannels` alongside accessibility preferences. `PUT /api/v1/users/{userId}/notification-preferences` replaces the quiet interval and channel list atomically. The channel identifiers are `PUSH`, `SMS`, `EMAIL` and `CALL`; each type appears at most once. A missing channel is disabled. New product defaults list all four channels: PUSH enabled, SMS, EMAIL and CALL disabled. Reading a saved channel list preserves its recorded choices, including older lists with three channels.

Reads return saved preferences or product defaults without creating a database record. Cross-context voice and notification reads use the same preference selection. The combined quiet-hours/channel replacement uses one owner-scoped command and one aggregate save, preserving notification timing, contacts and accessibility choices.

Preference mutations reserve the logical user before loading state, including the first creation. Updates to different settings therefore preserve one another's committed choices. Initialization uses the same reservation and preserves existing settings. A failed command rolls back preference values; its separate technical coordination key may remain without creating a preference record.

Existing preference updates preserve the stored identity, owner and creation timestamp. The persistence adapter applies mutable choices to the locked row and retains the ordering of emergency contacts. Channel values, timing and contacts have separate persistence representations within this context.

Public application reads and changes return immutable preference snapshots. REST mapping preserves the existing response fields; technical preference identity and creation timestamps stay outside the public JSON representation.

```json
{
  "quietHours": {"start": "22:00", "end": "07:00"},
  "channels": [
    {"type": "PUSH", "enabled": true},
    {"type": "SMS", "enabled": false},
    {"type": "EMAIL", "enabled": false},
    {"type": "CALL", "enabled": false}
  ]
}
```

Send `quietHours: null` to disable the interval. Start and end must differ and have minute precision; nonzero seconds or fractions are rejected without rounding. Overnight intervals are valid. The response contains `notificationChannels`, with the saved enabled state. Configuring a channel records a user preference; it does not establish delivery, a provider connection or receipt of a notification.

Every supplied channel requires an explicit boolean `enabled`; omission or null returns `400`. The accessibility replacements at `/preferences/contrast`, `/preferences/dark-theme`, `/preferences/reduced-motion`, `/preferences/reading-assistance` and `/preferences/voice-confirmation` also require `{"enabled":true}` or `{"enabled":false}`. Invalid input preserves saved choices.

Android presents the four channel icons and opens native switches from Editar. Its quiet-hours editor uses native time pickers. Both retain the same read and update endpoints.

## Delivery policy and emergency contacts

`PUT /api/v1/users/{userId}/notification-preferences/policy` atomically replaces notification timing and the user's emergency recipients. It preserves quiet hours, channels and accessibility settings. The authenticated user must own `userId`; another user cannot read or modify these preferences. `GET /api/v1/users/{userId}/preferences` includes `notificationPolicy` and `emergencyContacts`.

```json
{
  "policy": {
    "reminderMinutes": 10,
    "pendingAlertMinutes": 20,
    "omittedAlertMinutes": 120,
    "escalationMinutes": 180,
    "dailySummaryTime": "20:00"
  },
  "contacts": [{"name": "Rosa", "phone": "+51999123456"}]
}
```

The four delays are minutes from scheduled intake time. They must increase strictly from reminder to escalation and remain between 1 and 1440 minutes. Summary time has minute precision; null disables the summary. Contacts have a name of up to 80 characters and a phone of 7 to 15 digits with an optional leading plus. Spaces, parentheses and hyphens are removed. At most five distinct phone numbers are accepted; an empty list removes all recipients. Invalid requests preserve the complete existing policy and contacts.

Existing preference rows without policy columns use product defaults (10, 20, 120 and 180 minutes, summary at 20:00); they have no invented recipients. Policy and ordered contacts are persisted independently of the domain types. Configuring a recipient does not grant care-link consent or health-data access. This endpoint stores configuration; the current omission scheduler still uses its existing grace and escalation settings, and daily summary or contact delivery is not enabled by this endpoint alone.

Stored quiet intervals require both boundaries. A saved timing policy requires all four delays; only entirely absent legacy policy values use defaults. Incomplete or invalid saved settings raise a persistence-state failure without rewriting the record. `docs/database/preferences-preflight.sql` reports affected counts through a read-only PostgreSQL transaction before schema adoption.

Preference writes preserve the stored identity and original creation time. The persistence adapter records the shared application-clock time in updated_at for every deliberate save, including changes limited to channel or contact collections. Read-only lookups preserve both audit timestamps.

The repository reconstructs preference values and ordered contacts inside its read transaction. Callers receive domain values that remain usable after that transaction closes; HTTP rendering does not require an open persistence session.

The preference snapshot includes `darkTheme`. `PUT /api/v1/users/{userId}/preferences/dark-theme` replaces only that owner's theme choice with an explicit `enabled` boolean. New users and legacy stored null values read as false. The update uses the same per-user mutation reservation and preserves other accessibility and notification settings.
