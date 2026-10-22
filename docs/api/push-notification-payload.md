# Push notification data contract

FCM device messages carry a string-valued `data` object. Android uses localized resources for display text and identifies retries by `deliveryId`. A provider acceptance receipt does not establish handset display or clinical confirmation.

| Field | Value |
| --- | --- |
| schemaVersion | `1` |
| deliveryId | Stable persisted installation-delivery UUID retained on retry |
| intakeId | Source Intake Execution identifier |
| olderAdultId | Adult whose intake produced the notification |
| kind | `REINFORCED_REMINDER` or `CAREGIVER_ALERT` |
| medicationName | Exact medication name snapshot from the omission case |
| scheduledAt | Original scheduled instant formatted as ISO 8601 UTC |

The FCM envelope targets the currently resolved device registration token. The data object contains no session reference, caregiver identifier, credential, dosage interpretation or translated display sentence. Token rotation under the same authorized session preserves `deliveryId` and source data. A replacement session does not inherit queued work selected for the old session.

The receiver validates the schema, required fields and notification kind before display. It suppresses repeated delivery IDs and verifies local account scope. Navigation uses the structured adult and intake identifiers; the application does not infer routing from a topic name or an English message body.

## Transport configuration

`tata.notifications.firebase.enabled=true` enables FCM with `tata.notifications.firebase.project-id` and Google Application Default Credentials supplied by the deployment. No service account file is stored in the repository. The transport has a five-second connection timeout and a fifteen-second read timeout. Disabled transport produces a retryable failure rather than an acceptance receipt.

Each device attempt reserves durable work, revalidates its address and notification preferences, and invokes the provider outside the database transaction. Accepted receipts are retained per installation. Provider acceptance followed by a process interruption can produce a repeated external message, so receiving devices use the stable delivery ID for duplicate suppression.

## Recovery

The recovery scan finds pending reminders and active omission alerts without a durable intention. It invokes the same locked command handlers used by source events, preserving existing alert identifiers, generation times and historical failure evidence. Existing intentions are excluded regardless of technical state, so recovery does not reopen cancelled or completed work. Scalar ID cursors and per-case transactions allow later records to progress when one source is inconsistent.

Quiet hours use the explicit deployment zone `tata.notifications.quiet-hours-zone`, defaulting to `America/Lima`. Preference intervals have distinct minute-precision boundaries, with an inclusive start and exclusive end; overnight intervals cross midnight. The preference contract contains no per-user timezone.
