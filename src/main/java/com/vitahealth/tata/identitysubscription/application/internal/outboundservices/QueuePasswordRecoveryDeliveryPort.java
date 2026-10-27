package com.vitahealth.tata.identitysubscription.application.internal.outboundservices;

import java.time.Instant;

/** Commits an encrypted recovery intention in the same transaction as its challenge. */
public interface QueuePasswordRecoveryDeliveryPort {
  void queueRecovery(
      String accountId, String email, String code, String challengeHash, Instant expiresAt);
}
