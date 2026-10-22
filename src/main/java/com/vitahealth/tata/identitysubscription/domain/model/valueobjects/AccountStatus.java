package com.vitahealth.tata.identitysubscription.domain.model.valueobjects;

/** Lifecycle states that determine whether caregiver credentials may authenticate. */
public enum AccountStatus {
  /** Email ownership has not yet been verified. */
  PENDING_VERIFICATION,
  /** Account verification is complete and authentication is permitted. */
  ACTIVE,
  /** Authentication is disabled for the account. */
  DISABLED
}
