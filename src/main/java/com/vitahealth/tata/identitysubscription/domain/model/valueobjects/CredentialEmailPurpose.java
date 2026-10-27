package com.vitahealth.tata.identitysubscription.domain.model.valueobjects;

/** Prevents an activation code from being treated as password-recovery evidence. */
public enum CredentialEmailPurpose {
  ACCOUNT_VERIFICATION,
  PASSWORD_RECOVERY
}
