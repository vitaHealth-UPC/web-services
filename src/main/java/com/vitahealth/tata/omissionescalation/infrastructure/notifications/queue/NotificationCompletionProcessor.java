package com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue;

import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationDeviceDeliveryJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationIntentJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Reflects actual provider outcomes into the source case only after durable fanout finishes. */
@Service
public class NotificationCompletionProcessor {
  private final NotificationIntentJpaRepository intents;
  private final NotificationDeviceDeliveryJpaRepository deliveries;
  private final IOmissionCaseRepository cases;
  private final EntityManager entities;

  /**
   * Injects owned storage; no provider or cross-context operation occurs during source completion.
   */
  public NotificationCompletionProcessor(
      NotificationIntentJpaRepository intents,
      NotificationDeviceDeliveryJpaRepository deliveries,
      IOmissionCaseRepository cases,
      EntityManager entities) {
    this.intents = intents;
    this.deliveries = deliveries;
    this.cases = cases;
    this.entities = entities;
  }

  /**
   * Commits source evidence and technical completion once, retaining the original business alert.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean complete(String intentId) {
    var snapshot = intents.findById(intentId);
    if (snapshot.isEmpty()) return false;
    var source =
        cases
            .findByIdForUpdate(snapshot.orElseThrow().getOmissionCaseId())
            .orElseThrow(() -> new IllegalStateException("notification source is missing"));
    var intent = intents.findForUpdate(intentId).orElseThrow();
    entities.refresh(intent, LockModeType.PESSIMISTIC_WRITE);
    if (intent.getState() != NotificationIntentState.FANOUT_COMPLETE) return false;
    boolean reminder = intent.getKind() == NotificationKind.REINFORCED_REMINDER;
    if (reminder
        ? source.getStatus() != OmissionCaseStatus.PENDING
        : source.getStatus() != OmissionCaseStatus.OMITTED
            && source.getStatus() != OmissionCaseStatus.ESCALATED) {
      intent.cancel();
      return false;
    }
    if (deliveries.existsByIntent_IdAndState(intentId, NotificationDeliveryState.READY))
      return false;
    var accepted = deliveries.findLatestAcceptance(intentId, NotificationDeliveryState.ACCEPTED);
    if (accepted.isEmpty()) {
      intent.cancel();
      return false;
    }
    if (reminder) {
      if (source.getReinforcedReminderSentAt() == null)
        source.markReminderSent(accepted.orElseThrow());
    } else {
      source.markAlertSent(accepted.orElseThrow());
    }
    cases.save(source);
    if (!intent.completeDelivery())
      throw new IllegalStateException("notification completion state changed");
    return true;
  }
}
