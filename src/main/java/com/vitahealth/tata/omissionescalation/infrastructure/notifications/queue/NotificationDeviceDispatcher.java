package com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationDeviceTransport;
import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationRecipientPort;
import com.vitahealth.tata.omissionescalation.application.result.NotificationDispatchRequest;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.domain.ports.INotificationPreferencesPort;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationDeviceDeliveryJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationIntentJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reserves durable work, calls the provider outside transactions and commits only owned outcomes.
 */
@Service
public class NotificationDeviceDispatcher {
  private final NotificationDeviceDeliveryJpaRepository deliveries;
  private final NotificationIntentJpaRepository intents;
  private final IOmissionCaseRepository cases;
  private final NotificationRecipientPort recipients;
  private final INotificationPreferencesPort preferences;
  private final NotificationDeviceTransport transport;
  private final EntityManager entities;
  private final Clock clock;
  private final TransactionTemplate transaction;

  /** Injects independently committed reservations and external dispatch responsibilities. */
  public NotificationDeviceDispatcher(
      NotificationDeviceDeliveryJpaRepository deliveries,
      NotificationIntentJpaRepository intents,
      IOmissionCaseRepository cases,
      NotificationRecipientPort recipients,
      INotificationPreferencesPort preferences,
      NotificationDeviceTransport transport,
      EntityManager entities,
      Clock clock,
      PlatformTransactionManager manager) {
    this.deliveries = deliveries;
    this.intents = intents;
    this.cases = cases;
    this.recipients = recipients;
    this.preferences = preferences;
    this.transport = transport;
    this.entities = entities;
    this.clock = clock;
    transaction = new TransactionTemplate(manager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Dispatches at most one attempt; retry/crash recovery retains its original stable delivery ID.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void dispatch(String deliveryId) {
    var worker = UUID.randomUUID().toString();
    var selected = transaction.execute(status -> reserve(deliveryId, worker));
    if (selected == null) return;
    var address =
        selected.kind() == NotificationKind.REINFORCED_REMINDER
            ? recipients.findOlderAdultDevice(
                selected.adultId(), selected.installationId(), selected.sessionId())
            : recipients.findCaregiverDevice(
                selected.adultId(),
                selected.recipientId(),
                selected.installationId(),
                selected.sessionId());
    if (address.isEmpty()) {
      finish(deliveryId, worker, null, true);
      return;
    }
    var policy = preferences.getPreferences(selected.recipientId());
    if (!policy.pushEnabled() || policy.isQuietAt(clock.instant())) {
      finish(deliveryId, worker, null, false);
      return;
    }
    if (!Boolean.TRUE.equals(
        transaction.execute(status -> confirmEligibility(deliveryId, worker, selected)))) return;
    var request =
        new NotificationDispatchRequest(
            deliveryId,
            selected.intakeId(),
            selected.adultId(),
            selected.kind(),
            selected.medicationName(),
            selected.scheduledAt(),
            address.orElseThrow().token());
    String receipt;
    try {
      receipt = transport.send(request);
      if (receipt == null || receipt.isBlank() || receipt.length() > 512)
        throw new IllegalStateException("notification provider returned no valid receipt");
    } catch (RuntimeException providerFailure) {
      finish(deliveryId, worker, null, false);
      return;
    }
    // Preparation and persistence failures propagate; only actual transport failures become
    // retries.
    finish(deliveryId, worker, receipt, false);
  }

  private Selection reserve(String deliveryId, String worker) {
    var snapshot = deliveries.findById(deliveryId);
    if (snapshot.isEmpty()) return null;
    var intent =
        intents
            .findById(snapshot.orElseThrow().getIntentId())
            .orElseThrow(() -> new IllegalStateException("notification intention is missing"));
    var source =
        cases
            .findByIdForUpdate(intent.getOmissionCaseId())
            .orElseThrow(() -> new IllegalStateException("notification source is missing"));
    var delivery = deliveries.findForUpdate(deliveryId).orElseThrow();
    entities.refresh(delivery, LockModeType.PESSIMISTIC_WRITE);
    var now = clock.instant();
    if (!delivery.claim(worker, now, Duration.ofSeconds(60))) return null;
    boolean reminder = intent.getKind() == NotificationKind.REINFORCED_REMINDER;
    if (intent.getState() == NotificationIntentState.CANCELLED
        || (reminder
            ? source.getStatus() != OmissionCaseStatus.PENDING
            : source.getStatus() != OmissionCaseStatus.OMITTED
                && source.getStatus() != OmissionCaseStatus.ESCALATED)) {
      delivery.cancel();
      return null;
    }
    return new Selection(
        source.getId(),
        delivery.getInstallationId(),
        delivery.getSessionId(),
        delivery.getRecipientId(),
        source.getOlderAdultId(),
        source.getIntakeId(),
        source.getMedicationName(),
        source.getScheduledAt(),
        intent.getKind());
  }

  private boolean confirmEligibility(String deliveryId, String worker, Selection selected) {
    var source =
        cases
            .findByIdForUpdate(selected.sourceCaseId())
            .orElseThrow(() -> new IllegalStateException("notification source is missing"));
    var delivery = deliveries.findForUpdate(deliveryId).orElse(null);
    if (delivery == null) return false;
    entities.refresh(delivery, LockModeType.PESSIMISTIC_WRITE);
    if (!delivery.owns(worker, clock.instant())) return false;
    boolean reminder = selected.kind() == NotificationKind.REINFORCED_REMINDER;
    if (reminder
        ? source.getStatus() != OmissionCaseStatus.PENDING
        : source.getStatus() != OmissionCaseStatus.OMITTED
            && source.getStatus() != OmissionCaseStatus.ESCALATED) {
      delivery.cancel();
      return false;
    }
    return true;
  }

  private void finish(String deliveryId, String worker, String receipt, boolean cancelled) {
    transaction.executeWithoutResult(
        status ->
            deliveries
                .findForUpdate(deliveryId)
                .ifPresent(
                    delivery -> {
                      entities.refresh(delivery, LockModeType.PESSIMISTIC_WRITE);
                      var now = clock.instant();
                      if (!delivery.owns(worker, now)) return;
                      if (cancelled) delivery.cancel();
                      else if (receipt == null) delivery.retry(worker, now);
                      else delivery.accept(worker, now, receipt);
                    }));
  }

  private record Selection(
      long sourceCaseId,
      String installationId,
      String sessionId,
      String recipientId,
      String adultId,
      String intakeId,
      String medicationName,
      Instant scheduledAt,
      NotificationKind kind) {
    @Override
    public String toString() {
      return "NotificationSelection[redacted]";
    }
  }
}
