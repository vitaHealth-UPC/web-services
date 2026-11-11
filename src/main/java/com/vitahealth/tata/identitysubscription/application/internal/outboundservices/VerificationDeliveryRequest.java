package com.vitahealth.tata.identitysubscription.application.internal.outboundservices;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.CredentialEmailPurpose;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.VerificationCode;
import java.time.Instant;
import java.util.Objects;

/** A decrypted delivery attempt whose stable identifier is reused for provider retries. */
public record VerificationDeliveryRequest(
    String deliveryId,
    String email,
    String code,
    Instant expiresAt,
    CredentialEmailPurpose purpose) {
  public VerificationDeliveryRequest(
      String deliveryId, String email, String code, Instant expiresAt) {
    this(deliveryId, email, code, expiresAt, CredentialEmailPurpose.ACCOUNT_VERIFICATION);
  }

  /** Validates the immutable request before invoking a provider. */
  public VerificationDeliveryRequest {
    if (deliveryId == null || !deliveryId.matches("[A-Za-z0-9_-]{1,128}")) {
      throw new IllegalArgumentException("Invalid verification delivery identifier");
    }
    email = new EmailAddress(email).value();
    code = new VerificationCode(code).value();
    Objects.requireNonNull(expiresAt, "expiresAt");
    Objects.requireNonNull(purpose, "purpose");
  }

  /** Omits the recipient and code from diagnostics. */
  @Override
  public String toString() {
    return "VerificationDeliveryRequest[deliveryId="
        + deliveryId
        + ", expiresAt="
        + expiresAt
        + "]";
  }
}
