package com.vitahealth.tata.omissionescalation.application.internal.commandservices;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.IDomainEventPublisher;
import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationIntentStore;
import com.vitahealth.tata.omissionescalation.application.result.NotificationIntentRequest;
import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.commands.GenerateCaregiverAlertCommand;
import com.vitahealth.tata.omissionescalation.domain.model.entities.CareAlert;
import com.vitahealth.tata.omissionescalation.domain.model.events.CaregiverAlertGenerated;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates one business alert and atomically queues its durable device delivery work. */
@Service
public class GenerateCaregiverAlertCommandHandler {

  private final IOmissionCaseRepository repository;
  private final NotificationIntentStore intentions;
  private final IDomainEventPublisher eventPublisher;

  /** Injects locked case storage, the delivery port, and synchronous business event publication. */
  public GenerateCaregiverAlertCommandHandler(
      IOmissionCaseRepository repository,
      NotificationIntentStore intentions,
      IDomainEventPublisher eventPublisher) {
    this.repository = repository;
    this.intentions = intentions;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Creates and publishes the alert once; repeated requests retain its identity and original
   * generation time. Device dispatch owns retries and acceptance evidence after commit. Sent or
   * inactive alerts are skipped.
   */
  @Transactional
  public void handle(GenerateCaregiverAlertCommand command) {
    OmissionCase omissionCase = repository.findByIdForUpdate(command.omissionCaseId()).orElse(null);
    if (omissionCase == null
        || (omissionCase.getStatus() != OmissionCaseStatus.OMITTED
            && omissionCase.getStatus() != OmissionCaseStatus.ESCALATED)) {
      return;
    }
    boolean created = omissionCase.getAlerts().isEmpty();
    CareAlert alert =
        created ? omissionCase.addAlert(command.now()) : omissionCase.getAlerts().getFirst();
    if (alert.getStatus() == AlertStatus.SENT) return;
    if (created) repository.save(omissionCase);
    intentions.enqueue(
        new NotificationIntentRequest(
            omissionCase.getId(), NotificationKind.CAREGIVER_ALERT, command.now()));
    if (created)
      eventPublisher.publish(
          new CaregiverAlertGenerated(
              omissionCase.getId(),
              omissionCase.getIntakeId(),
              omissionCase.getOlderAdultId(),
              command.now()));
  }
}
