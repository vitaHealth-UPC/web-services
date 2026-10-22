package com.vitahealth.tata.identitysubscription.application.authentication;

import java.time.Instant;
import java.util.Objects;

/** Verified session identity independent of servlet and Spring Security principal types. */
public record AuthenticatedSession(
    String subjectId, Role role, Instant expiresAt, String careLinkId) {
  /** Requires an explicit identity and the relationship scope of adult/setup credentials. */
  public AuthenticatedSession {
    if (subjectId == null || subjectId.isBlank()) {
      throw new IllegalArgumentException("subjectId is required");
    }
    Objects.requireNonNull(role, "role");
    Objects.requireNonNull(expiresAt, "expiresAt");
    if (role != Role.CAREGIVER && (careLinkId == null || careLinkId.isBlank())) {
      throw new IllegalArgumentException("careLinkId is required for adult and setup sessions");
    }
  }

  /** Distinguishes caregiver, adult and restricted consent-onboarding credentials. */
  public enum Role {
    CAREGIVER,
    OLDER_ADULT,
    LINK_SETUP
  }
}
