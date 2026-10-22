# Confirmed care links

Public failures retain stable machine-readable codes in `title` and HTTP status. Their `detail` message follows `Accept-Language`, with English and Spanish catalogs and English fallback. Validation messages omit submitted personal data and credentials.

`GET /api/v1/care-links?caregiverId={caregiverId}` returns the caregiver's confirmed links with accepted consent, ordered by confirmation time descending and then identifier ascending.

Each item contains `id`, `olderAdultId`, `olderAdultName` and `confirmedAt`. Linking codes are excluded. Pending or unconsented links are excluded. A caregiver without confirmed links receives HTTP 200 with an empty array; a missing or blank caregiver identifier receives HTTP 400.

The existing generation, acceptance, consent and individual lookup contracts remain available.

## Caregiver-owned profile changes

`PUT /api/v1/older-adults/{olderAdultId}` accepts `fullName` and `birthDate`. The registering caregiver can update the profile before or after consent. The caregiver identity comes from the authenticated session. Profile identity, registering caregiver and original creation timestamp remain unchanged.

`PUT /api/v1/older-adults/{olderAdultId}/emergency-contact` accepts a complete `fullName`, `relationship` and `phone`. It returns the updated profile and preserves its basic data. Contact names, relationships and phone fields have maximum lengths of 160, 80 and 40 characters. Associating a contact does not grant consent or make the contact a notification recipient.

Both operations return HTTP 200 with the profile resource. Missing or invalid fields, including a future birth date, return HTTP 400 without changing stored data. Other caregivers and adult or onboarding sessions cannot perform these changes. Application handlers also check registration ownership, independently of HTTP authorization, and reserve the profile row inside the write transaction.

## Relationship-scoped sessions

Consent confirmation issues an adult credential for the exact confirmed relationship. JWT and opaque token providers require that relationship to belong to the adult and retain accepted consent and confirmation evidence. The Care Link session adapter issues restricted onboarding credentials only for an accepted request in `AWAITING_CONSENT`.

Repeating an approved decision returns the existing confirmed relationship without replacing its decision or confirmation time, publishing another confirmation event or issuing another credential. The request uses a currently valid adult session; the response has no new access token, and the client retains its existing session. The original restricted onboarding credential no longer authorizes requests after confirmation.

Repeating a rejection through the application command returns its original decision evidence. An opposite decision cannot replace recorded approval or rejection. Confirmation uses a dedicated handler inside the consent transaction; failed confirmation consumers or credential issuance roll back both the intermediate approval and final confirmation.

Live session authorization uses an optional relationship query. An absent relationship invalidates the credential; a failed persistence lookup propagates as a technical failure rather than being treated as missing consent.

## Stored lifecycle evidence

Restored relationships validate status against code usage, consent, confirmation and domain timestamps. A pending request has no usage or decision; an accepted request retains usage evidence; confirmation requires approval; rejection or withdrawal retains a recorded negative decision. Withdrawal preserves any earlier confirmation. An expired code remains valid historical evidence after its acceptance.

Managed updates preserve relationship identity, members, code, expiration and creation time. They cannot erase usage or confirmation evidence, move update time backwards, or reactivate a revoked relationship through an older snapshot.

## Linking-code allocation

`POST /api/v1/care-links/linking-codes` issues a `TATA-0000` format code with a fifteen-minute lifetime. Issuance reserves the code namespace inside the caller's transaction, frees expired reservations and skips occupied candidates before inserting a relationship. The database enforces uniqueness of active reservations through `reserved_linking_code`; `linking_code` retains historical evidence after acceptance or expiration.

Code lookup selects the current reservation before historical evidence. Reusing a freed code creates a different relationship identity and preserves the original row. If 64 candidates cannot be allocated, the operation returns HTTP 503 with `LINKING_CODE_UNAVAILABLE` and creates no partial relationship. Startup reconstructs live reservations under the same namespace lock and rejects ambiguous active legacy codes.

## Requests for the same caregiver and adult

A caregiver/adult pair has at most one live pending request, accepted request awaiting consent, or confirmed relationship. Repeating code issuance for a live pending request returns its original identity, code and expiration without generating another candidate or extending its lifetime.

Issuance for a pair awaiting consent or already confirmed returns HTTP 409 with `CARE_LINK_ALREADY_EXISTS`. Once a pending code reaches its exclusive expiration, or consent is rejected or withdrawn, another request can use a new identity. Earlier code, decision and confirmation evidence remain stored.

The issuer reserves the code namespace and locks existing open relationships. The database also enforces the pair reservation through `uk_care_link_reserved_members`. Acceptance retains the pair reservation after the original code expires; rejection and withdrawal release it. Startup restores missing reservations transactionally and rejects ambiguous live pairs without choosing or deleting historical rows.

## Adult consent withdrawal

`DELETE /api/v1/care-links/{careLinkId}/consent` returns HTTP 204 when the linked adult withdraws confirmed consent. The actor comes from the authenticated older-adult session; the request has no actor field or body. Caregiver, foreign-adult and restricted onboarding access cannot perform this operation.

The relationship becomes `REVOKED`, retains its creation and confirmation timestamps, and stores the original approval and withdrawal in `care_link_consent_withdrawals`. Relationship state, decision evidence and removal of all credentials scoped to that link commit in one transaction. A failed event consumer rolls back the operation. Caregiver credentials remain valid, while caregiver access to this adult requires another active consented relationship.

The client clears its revoked adult credentials after the successful response. Reusing them returns HTTP 401. Repeating the application command does not create a new decision or change the original withdrawal time.
