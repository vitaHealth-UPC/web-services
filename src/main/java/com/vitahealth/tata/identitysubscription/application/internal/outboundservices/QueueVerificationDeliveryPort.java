package com.vitahealth.tata.identitysubscription.application.internal.outboundservices;

import java.time.Instant;

/** Records a verification delivery intention within the account command's transaction. */
public interface QueueVerificationDeliveryPort {
  /**
   * Persists an encrypted code and the challenge identity needed to reject superseded deliveries.
   */
  void queue(String accountId, String email, String code, String challengeHash, Instant expiresAt);
}
