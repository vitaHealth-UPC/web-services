package com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices;

/** Reads current consented access through the Care Link application boundary. */
@FunctionalInterface
public interface CareLinkVerificationPort {
  /** Returns false for unavailable relationships; technical failures must propagate. */
  boolean isAuthorized(String caregiverId, String olderAdultId);
}
