package com.vitahealth.tata.identitysubscription.application.internal.outboundservices;

/** Protects temporary verification codes stored in durable delivery requests. */
public interface VerificationPayloadCipher {
  /** Encrypts a code bound to its stable delivery identifier. */
  String encrypt(String deliveryId, String code);

  /** Authenticates and decrypts a payload for the same delivery identifier. */
  String decrypt(String deliveryId, String encryptedPayload);
}
