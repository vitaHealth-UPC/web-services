package com.vitahealth.tata.identitysubscription.application.internal.outboundservices;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import java.util.Objects;

/** Only a token verifier may produce this identity; raw client profile fields are not evidence. */
public record VerifiedSocialIdentity(
    String issuer,
    String subject,
    Provider provider,
    String email,
    boolean emailVerified,
    String displayName) {
  public enum Provider {
    GOOGLE,
    FACEBOOK
  }

  public VerifiedSocialIdentity {
    if (issuer == null
        || issuer.isBlank()
        || subject == null
        || subject.isBlank()
        || subject.length() > 128)
      throw new IllegalArgumentException("Invalid social identity binding");
    Objects.requireNonNull(provider, "provider");
    if (email != null) email = new EmailAddress(email).value();
    if (emailVerified && email == null)
      throw new IllegalArgumentException("Verified email requires an address");
    if (displayName != null && displayName.length() > 120)
      displayName = displayName.substring(0, 120);
  }

  @Override
  public String toString() {
    return "VerifiedSocialIdentity[provider=" + provider + ", personalData=REDACTED]";
  }
}
