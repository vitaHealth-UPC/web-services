package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import java.time.Instant;

/** Public session identity response without bearer credentials or persistence details. */
public record CurrentSessionResource(
    String subjectId, String role, Instant expiresAt, String careLinkId) {}
