package com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationIntentStore;
import com.vitahealth.tata.omissionescalation.application.result.NotificationIntentReceipt;
import com.vitahealth.tata.omissionescalation.application.result.NotificationIntentRequest;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.NotificationIntentPersistenceEntity;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationIntentJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.OmissionCaseJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Atomically records one source-kind intention without calling a provider or claiming delivery. */
@Component
public class PersistedNotificationIntentStore implements NotificationIntentStore {
  private final NotificationIntentJpaRepository intents;
  private final OmissionCaseJpaRepository cases;

  /** Injects source reservation and durable intention storage within this bounded context. */
  public PersistedNotificationIntentStore(
      NotificationIntentJpaRepository intents, OmissionCaseJpaRepository cases) {
    this.intents = intents;
    this.cases = cases;
  }

  /** Serializes new-intent creation on the existing source row in the caller's transaction. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public NotificationIntentReceipt enqueue(NotificationIntentRequest request) {
    var source =
        cases
            .findByIdForUpdate(request.omissionCaseId())
            .orElseThrow(() -> new IllegalStateException("notification source does not exist"));
    var intent =
        intents
            .findBySourceCaseIdAndKind(request.omissionCaseId(), request.kind())
            .orElseGet(
                () ->
                    intents.saveAndFlush(
                        new NotificationIntentPersistenceEntity(
                            source, request.kind(), request.requestedAt())));
    return new NotificationIntentReceipt(intent.getId(), intent.getRequestedAt());
  }
}
