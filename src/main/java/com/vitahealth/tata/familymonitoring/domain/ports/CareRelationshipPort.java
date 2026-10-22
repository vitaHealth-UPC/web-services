package com.vitahealth.tata.familymonitoring.domain.ports;

/** Reads current consent through the public Care Link boundary without accessing its storage. */
public interface CareRelationshipPort {
  /**
   * Checks exact relationship ownership and current consent without accepting a replacement link.
   */
  boolean isRelationshipAuthorized(String careLinkId, String caregiverId, String olderAdultId);

  /** Checks pair-level access; historical follow-up selection requires the exact-link operation. */
  boolean isAuthorized(String caregiverId, String olderAdultId);
}
