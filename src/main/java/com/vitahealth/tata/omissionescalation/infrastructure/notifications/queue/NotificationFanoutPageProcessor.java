package com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationRecipientPort;
import com.vitahealth.tata.omissionescalation.application.result.NotificationDevicePage;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.NotificationDeviceDeliveryPersistenceEntity;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.NotificationIntentPersistenceEntity;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationDeviceDeliveryJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationIntentJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Commits one bounded recipient page and its cursor atomically without contacting a push provider.
 */
@Service
public class NotificationFanoutPageProcessor {
  private final NotificationIntentJpaRepository intents;
  private final NotificationDeviceDeliveryJpaRepository deliveries;
  private final IOmissionCaseRepository cases;
  private final NotificationRecipientPort recipients;
  private final EntityManager entities;
  private final Clock clock;

  /**
   * Injects owned persistence and public recipient boundaries for one independently committed page.
   */
  public NotificationFanoutPageProcessor(
      NotificationIntentJpaRepository intents,
      NotificationDeviceDeliveryJpaRepository deliveries,
      IOmissionCaseRepository cases,
      NotificationRecipientPort recipients,
      EntityManager entities,
      Clock clock) {
    this.intents = intents;
    this.deliveries = deliveries;
    this.cases = cases;
    this.recipients = recipients;
    this.entities = entities;
    this.clock = clock;
  }

  /**
   * Reserves source before intention, matching enqueue order and retaining partial fanout on retry.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void process(String intentId) {
    var snapshot = intents.findById(intentId);
    if (snapshot.isEmpty()) return;
    var source =
        cases
            .findByIdForUpdate(snapshot.orElseThrow().getOmissionCaseId())
            .orElseThrow(() -> new IllegalStateException("notification source is missing"));
    var intent = intents.findForUpdate(intentId).orElseThrow();
    entities.refresh(intent, LockModeType.PESSIMISTIC_WRITE);
    var now = clock.instant();
    if (intent.getState() != NotificationIntentState.READY) return;
    boolean reminder = intent.getKind() == NotificationKind.REINFORCED_REMINDER;
    if (reminder
        ? source.getStatus() != OmissionCaseStatus.PENDING
        : source.getStatus() != OmissionCaseStatus.OMITTED
            && source.getStatus() != OmissionCaseStatus.ESCALATED) {
      intent.cancel();
      return;
    }
    var worker = UUID.randomUUID().toString();
    if (!intent.claimFanout(worker, now, Duration.ofSeconds(60))) return;
    if (reminder) {
      var page =
          recipients.findOlderAdultDevices(
              source.getOlderAdultId(), intent.getDeviceAfterId(), 100);
      materialize(intent, source.getOlderAdultId(), page, clock.instant());
      now = clock.instant();
      if (page.nextCursor() == null) finishScan(intent, worker, now);
      else {
        requireProgress(intent.advanceFanout(worker, now, "", null, page.nextCursor()));
        requireProgress(intent.rescheduleFanout(worker, now, Duration.ZERO));
      }
      return;
    }
    var caregiver = intent.getCurrentCaregiverId();
    if (caregiver == null) {
      var page =
          recipients.findCaregivers(source.getOlderAdultId(), intent.getCaregiverAfterId(), 1);
      now = clock.instant();
      if (page.caregiverIds().size() > 1)
        throw new IllegalStateException("caregiver page exceeds delivery bound");
      if (page.caregiverIds().isEmpty()) {
        if (page.nextCursor() == null) finishScan(intent, worker, now);
        else {
          requireProgress(intent.advanceFanout(worker, now, page.nextCursor(), null, ""));
          requireProgress(intent.rescheduleFanout(worker, now, Duration.ZERO));
        }
        return;
      }
      caregiver = page.caregiverIds().getFirst();
    }
    var page =
        recipients.findCaregiverDevices(
            source.getOlderAdultId(), caregiver, intent.getDeviceAfterId(), 100);
    materialize(intent, caregiver, page, clock.instant());
    now = clock.instant();
    if (page.nextCursor() == null)
      requireProgress(intent.advanceFanout(worker, now, caregiver, null, ""));
    else
      requireProgress(
          intent.advanceFanout(
              worker, now, intent.getCaregiverAfterId(), caregiver, page.nextCursor()));
    requireProgress(intent.rescheduleFanout(worker, now, Duration.ZERO));
  }

  private void materialize(
      NotificationIntentPersistenceEntity intent,
      String recipient,
      NotificationDevicePage page,
      Instant now) {
    if (page.devices().size() > 100)
      throw new IllegalStateException("recipient page exceeds delivery bound");
    for (var device : page.devices()) {
      if (deliveries
          .findByIntent_IdAndInstallationId(intent.getId(), device.installationId())
          .isEmpty())
        deliveries.save(
            new NotificationDeviceDeliveryPersistenceEntity(
                intent, device.installationId(), device.sessionId(), recipient, now));
    }
    deliveries.flush();
  }

  private void finishScan(NotificationIntentPersistenceEntity intent, String worker, Instant now) {
    if (deliveries.existsByIntent_Id(intent.getId()))
      requireProgress(intent.completeFanout(worker, now));
    else {
      requireProgress(intent.advanceFanout(worker, now, "", null, ""));
      requireProgress(intent.rescheduleFanout(worker, now, Duration.ofSeconds(30)));
    }
  }

  private static void requireProgress(boolean applied) {
    if (!applied) throw new IllegalStateException("notification fanout reservation was lost");
  }
}
