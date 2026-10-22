package com.vitahealth.tata.identitysubscription.infrastructure.email.queue;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.QueueVerificationDeliveryPort;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationPayloadCipher;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.VerificationDeliveryPersistenceEntity;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.VerificationDeliveryJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Enqueues encrypted delivery payloads in the caller's account transaction. */
@Component
public class PersistedVerificationDeliveryQueue implements QueueVerificationDeliveryPort {
  private final VerificationDeliveryJpaRepository repository;
  private final VerificationPayloadCipher cipher;
  private final Clock clock;

  /** Injects durable storage and external-key encryption. */
  public PersistedVerificationDeliveryQueue(
      VerificationDeliveryJpaRepository repository, VerificationPayloadCipher cipher, Clock clock) {
    this.repository = repository;
    this.cipher = cipher;
    this.clock = clock;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void queue(
      String accountId, String email, String code, String challengeHash, Instant expiresAt) {
    String id = UUID.randomUUID().toString();
    repository.save(
        new VerificationDeliveryPersistenceEntity(
            id,
            accountId,
            email,
            challengeHash,
            cipher.encrypt(id, code),
            expiresAt,
            clock.instant()));
  }
}
