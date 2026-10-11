# Notification preferences

`GET /api/v1/users/{userId}/preferences` returns the quiet interval and `notificationChannels` alongside accessibility preferences. `PUT /api/v1/users/{userId}/notification-preferences` replaces the quiet interval and channel list atomically. The channel identifiers are `PUSH`, `SMS`, `EMAIL` and `CALL`; each type appears at most once. A missing channel is disabled. Existing records and default preferences retain their previous channels.

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

Send `quietHours: null` to disable the interval. Start and end must differ; overnight intervals are valid. The response contains `notificationChannels`, with the saved enabled state. Configuring a channel records a user preference; it does not establish delivery, a provider connection or receipt of a notification.

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
