package com.tata.identitysubscription.application.models;
import java.security.Principal;
import java.time.Instant;
public record AuthenticatedSession(String subjectId, Role role, Instant expiresAt, String careLinkId) implements Principal {
 public enum Role { CAREGIVER, OLDER_ADULT, LINK_SETUP }
 @Override public String getName() { return subjectId; }
}
