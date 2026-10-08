# Confirmed care links

`GET /api/v1/care-links?caregiverId={caregiverId}` returns the caregiver's confirmed links with accepted consent, ordered by confirmation time descending and then identifier descending.

Each item contains `id`, `olderAdultId`, `olderAdultName` and `confirmedAt`. Linking codes are excluded. Pending or unconsented links are excluded. A caregiver without confirmed links receives HTTP 200 with an empty array; a missing or blank caregiver identifier receives HTTP 400.

The existing generation, acceptance, consent and individual lookup contracts remain available.
