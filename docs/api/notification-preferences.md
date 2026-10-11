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
