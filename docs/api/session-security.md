# Session credentials

The session service issues HS256 signed JWT credentials. Each credential carries a session ID,
subject, role, issue time and expiry. Adult and consent-onboarding credentials also carry the care
link reference. Credentials contain no password, verification code or clinical information.

Authentication verifies the signature and issuer, then checks the persisted credential hash,
expiry, current account status and effective care-link authorization. Deleting the persisted
credential revokes it even when its signed expiry has not been reached. A role in the JWT cannot
override the role and ownership stored for the session.

Existing opaque credentials retain their UUID.UUID format and use the same persisted revocation
and authorization checks. Only that exact format enters the legacy resolver. An invalid JWT never
falls back to opaque authentication.

## Signing configuration

Set `TATA_JWT_SECRET_BASE64` to a base64 encoded cryptographically random 32-byte key before starting
the application. The application does not supply a production default key. Keep the key stable
across instances and restarts, outside source control. Replacing it invalidates previously signed
credentials. The fixture key in `application-test.properties` is restricted to tests.

## HTTP contracts

Clients continue to send `Authorization: Bearer <accessToken>`. Account verification and PIN access
return the existing `accountId`, `accessToken` and `expiresAt` fields. Clients treat the token as an
opaque credential and do not derive permissions from its payload.

`GET /api/v1/sessions/current` returns the authenticated session identity without the credential.
`DELETE /api/v1/sessions` revokes the caller's credential and returns 204. Protected requests with
a revoked, expired or invalid credential return 401.

## Resource ownership

Private MVC routes and request bodies require an authenticated `SessionPrincipal`. Missing,
anonymous, unauthenticated or foreign principals cannot bypass ownership checks. The route policy
also restricts consent-onboarding credentials to their own relationship setup.

Path variables, query parameters and ownership selectors in request records are checked against
the verified subject. Caregiver access to adult clinical resources requires an authorized care
link. Adult sessions can access their own resources. Requests selecting another account, adult,
medication, treatment or intake return 403 with `RESOURCE_ACCESS_DENIED`; this includes identifiers
whose ownership cannot be established, without disclosing private resource existence.

Registration, verification, session access and the explicitly public catalog and health routes
remain available without a private session. HTTP integration tests install the security filter
chain and obtain credentials from real account and relationship fixtures.

## Enrollment conflicts

Account email and adult PIN enrollment each have a database uniqueness constraint. A concurrent
losing account registration returns `DUPLICATE_EMAIL`; a losing PIN enrollment returns
`PIN_ALREADY_REGISTERED`. Their transaction rolls back before the application maps the conflict.
The winning account or credential remains intact. Other integrity failures are not converted into
these expected conflicts.

The persistence adapters identify `uk_identity_accounts_email` and `uk_identity_pin_adult` using
SQLState 23505 and structured constraint metadata. Existing databases require schema migration to
these named constraints; these tests create isolated schemas and do not establish compatibility
with an existing deployed schema.

Registration preserves password content. The hashing adapter rejects credentials beyond its
supported byte length, and the command returns a validation failure rather than truncating them.
PIN enrollment and access require exactly four ASCII digits and preserve leading zeroes.
