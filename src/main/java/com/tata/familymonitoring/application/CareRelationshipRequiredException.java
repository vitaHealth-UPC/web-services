package com.tata.familymonitoring.application;

/** Public failure raised when a monitoring operation has no confirmed care relationship. */
public final class CareRelationshipRequiredException extends RuntimeException {
    public CareRelationshipRequiredException() {
        super("an active confirmed care relationship is required");
    }
}
