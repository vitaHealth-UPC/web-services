package com.tata.identitysubscription.application.models;

/**
 * Subject resolved from a persisted opaque session token.
 * For family accounts the id is the account UUID; for PIN sessions it is the older-adult id.
 */
public record AuthenticatedSubject(String subjectId) {
    public AuthenticatedSubject {
        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId is required");
        }
        subjectId = subjectId.trim();
    }
}
