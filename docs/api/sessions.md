# Session and resource authorization contract

`POST /api/v1/accounts`, `POST /api/v1/accounts/verification`, `POST /api/v1/email-verification-requests`, `POST /api/v1/sessions`, `POST /api/v1/pin-sessions` and `POST /api/v1/care-links/acceptances` are public credential-establishment operations. Plans, liveness endpoints (`/health`, `/actuator/health`) and OpenAPI documentation can be read publicly.

Other API operations require `Authorization: Bearer <accessToken>`. Tokens are opaque, persisted as SHA-256 hashes and validated on every protected request. An invalid, expired or revoked session returns `401` with code `AUTHENTICATION_REQUIRED`. Resource ownership violations return `403` with code `RESOURCE_ACCESS_DENIED`.

## Establishing sessions

- Email verification returns the existing account fields plus `accessToken` and `expiresAt`, establishing a caregiver session without a second sign-in.
- Password sign-in establishes a caregiver session. PIN sign-in establishes an older-adult session after verifying a confirmed care link.
- Accepting a linking code returns the existing care-link fields plus a 15-minute restricted session. It grants access only to that link, its adult profile and the consent operation.
- Confirming consent replaces the restricted session with a 12-hour older-adult session. The client persists the replacement credentials before entering the adult's home screen.
- Caregiver and older-adult sessions expire after 12 hours. Legacy persisted records without a role are rejected.

`GET /api/v1/sessions/current` returns the authenticated subject, role, expiration and logical care-link identifier. It never exposes credential hashes. `DELETE /api/v1/sessions` revokes the presented session and returns `204`.

## Ownership

Caregiver identifiers in route parameters, query parameters and request resources must match the authenticated caregiver. Reading or changing an adult's medications, treatments, inventory, intakes, adherence or monitoring requires a confirmed care relationship. The profile creator can read their profile and generate its linking code during onboarding. Caregivers cannot grant consent on behalf of the adult.

Older-adult sessions can access only their own resources. Their care link and caregiver account must remain active. Preference identifiers must match the authenticated subject. Medication, treatment and intake identifiers resolve their owners through public bounded-context facades before authorization.

PIN attempts use a persistence lock to serialize concurrent attempts. Incorrect attempts commit their counters even though authentication returns an error. The fourth incorrect attempt locks the credential for 15 minutes; a correct PIN does not bypass the lock.
