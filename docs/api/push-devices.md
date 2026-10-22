# Push device registrations

Device addresses belong to a revocable authenticated session. The API derives ownership from the bearer credential and accepts no account or session identifier in the request body. Caregiver and confirmed adult sessions can register; consent setup sessions cannot.

## Register or rotate

`PUT /api/v1/push-devices/{installationId}`

The installation ID is a canonical UUID created by the Android installation. The JSON body contains `token`, the exact current FCM registration token. A successful response is HTTP 200 with `installationId` and `updatedAt`; the device token and session reference are not returned.

Repeating the same token for the same session preserves the update time. Rotation retains the session and original creation time. A different active session or a token already assigned to another installation returns HTTP 409. A new authenticated session can replace an installation whose previous session is expired, revoked or no longer authorized.

## Unregister

`DELETE /api/v1/push-devices/{installationId}`

Removal requires the exact current session owner. Successful removal and retries after removal return HTTP 204. Another session receives HTTP 403. Missing, expired or revoked credentials receive HTTP 401.

## Delivery authorization

Registration is evidence of a device address, not provider acceptance or handset delivery. Notification dispatch must revalidate the stored session reference and current Care Link authorization before using an address. The registration API does not send a notification.

## Internal recipient lookup

Identity exposes bounded caregiver and adult address pages through its internal context facade, with at most 100 candidates per page. Each selected session is revalidated against its account and original consent scope. The cursor follows scanned installations, including candidates that lose authorization, so filtered pages do not repeat indefinitely. No HTTP endpoint lists these addresses.

Care Link supplies bounded, credential-free caregiver IDs for a specific adult. Omission checks the relationship again before resolving caregiver device addresses through Identity. Withdrawal excludes that caregiver from the adult's notification recipients while preserving the caregiver's independently authorized account session.
