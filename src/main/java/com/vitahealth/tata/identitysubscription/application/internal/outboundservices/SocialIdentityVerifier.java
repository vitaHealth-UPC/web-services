package com.vitahealth.tata.identitysubscription.application.internal.outboundservices;

/**
 * Validates signature, issuer, audience, expiry and allowed sign-in provider before returning
 * identity.
 */
public interface SocialIdentityVerifier {
  VerifiedSocialIdentity verify(String firebaseIdToken);
}
