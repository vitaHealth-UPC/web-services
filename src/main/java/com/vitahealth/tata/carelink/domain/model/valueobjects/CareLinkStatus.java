package com.vitahealth.tata.carelink.domain.model.valueobjects;

/**
 * Distinguishes unused requests, consent onboarding, active approval and rejected/withdrawn links.
 */
public enum CareLinkStatus {
  /** Unused linking evidence with no recorded adult decision. */
  PENDING,
  /** Accepted linking evidence before final consented confirmation. */
  AWAITING_CONSENT,
  /** Confirmed relationship with active adult approval. */
  CONFIRMED,
  /** Rejected request or confirmed relationship whose adult withdrew consent. */
  REVOKED
}
