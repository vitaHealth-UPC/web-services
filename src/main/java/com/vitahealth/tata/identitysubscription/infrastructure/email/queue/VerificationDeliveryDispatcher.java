package com.vitahealth.tata.identitysubscription.infrastructure.email.queue;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationDeliveryRequest;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationEmailProvider;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationPayloadCipher;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountJpaRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.VerificationDeliveryJpaRepository;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dispatches durable verification messages outside database locks with recoverable reservations.
 */
@Component
public class VerificationDeliveryDispatcher {
  private final VerificationDeliveryJpaRepository deliveries;
  private final AccountJpaRepository accounts;
  private final VerificationPayloadCipher cipher;
  private final VerificationEmailProvider provider;
  private final Clock clock;
  private final TransactionTemplate transaction;

  /** Injects persistence, provider and clock; each state transition commits independently. */
  public VerificationDeliveryDispatcher(
      VerificationDeliveryJpaRepository deliveries,
      AccountJpaRepository accounts,
      VerificationPayloadCipher cipher,
      VerificationEmailProvider provider,
      Clock clock,
      PlatformTransactionManager manager) {
    this.deliveries = deliveries;
    this.accounts = accounts;
    this.cipher = cipher;
    this.provider = provider;
    this.clock = clock;
    transaction = new TransactionTemplate(manager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /** Processes a bounded batch; retries use the same payload and provider idempotency key. */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Scheduled(
      fixedDelayString = "${tata.email.dispatch-delay:5000}",
      initialDelayString = "${tata.email.dispatch-initial-delay:5000}")
  public void dispatch() {
    var due =
        deliveries
            .findTop20ByCompletedFalseAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                clock.instant());
    for (var candidate : due) {
      String token = UUID.randomUUID().toString();
      var request = transaction.execute(status -> reserve(candidate.id(), token));
      if (request == null) {
        continue;
      }
      final String providerId;
      try {
        providerId = provider.send(request);
        if (providerId == null || providerId.isBlank()) {
          throw new IllegalStateException(
              "Verification provider returned no acceptance identifier");
        }
      } catch (RuntimeException exception) {
        transaction.executeWithoutResult(status -> finish(candidate.id(), token, null, false));
        continue;
      }
      transaction.executeWithoutResult(status -> finish(candidate.id(), token, providerId, true));
    }
  }

  private VerificationDeliveryRequest reserve(String id, String token) {
    var candidate = deliveries.findForUpdate(id);
    if (candidate.isEmpty()) {
      return null;
    }
    var delivery = candidate.get();
    var now = clock.instant();
    if (!delivery.claim(token, now)) {
      return null;
    }
    var account = accounts.findByIdForUpdate(delivery.accountId());
    if (!now.isBefore(delivery.expiresAt())
        || account.isEmpty()
        || account.get().getStatus() != AccountStatus.PENDING_VERIFICATION
        || !Objects.equals(account.get().getVerificationCodeHash(), delivery.challengeHash())
        || !Objects.equals(account.get().getVerificationExpiresAt(), delivery.expiresAt())) {
      delivery.complete(null);
      return null;
    }
    final String code;
    try {
      code = cipher.decrypt(delivery.id(), delivery.encryptedCode());
    } catch (IllegalArgumentException exception) {
      delivery.complete(null);
      return null;
    }
    return new VerificationDeliveryRequest(
        delivery.id(), delivery.email(), code, delivery.expiresAt());
  }

  private void finish(String id, String token, String providerId, boolean accepted) {
    deliveries
        .findForUpdate(id)
        .filter(delivery -> delivery.isOwnedBy(token))
        .ifPresent(
            delivery -> {
              if (accepted || !clock.instant().isBefore(delivery.expiresAt())) {
                delivery.complete(providerId);
              } else {
                delivery.retry(clock.instant());
              }
            });
  }
}
