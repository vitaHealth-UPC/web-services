package com.tata.omissionescalation.application.internal.commandservices;

import com.tata.omissionescalation.application.internal.outboundservices.IDomainEventPublisher;
import com.tata.omissionescalation.application.internal.outboundservices.INotificationPort;
import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.commands.GenerateCaregiverAlertCommand;
import com.tata.omissionescalation.domain.model.entities.CareAlert;
import com.tata.omissionescalation.domain.model.events.CaregiverAlertGenerated;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenerateCaregiverAlertCommandHandler {

  private final IOmissionCaseRepository repository;
  private final INotificationPort notificationPort;
  private final IDomainEventPublisher eventPublisher;

  public GenerateCaregiverAlertCommandHandler(
      IOmissionCaseRepository repository,
      INotificationPort notificationPort,
      IDomainEventPublisher eventPublisher) {
    this.repository = repository;
    this.notificationPort = notificationPort;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Creates the alert once and requests its delivery. A delivery failure is recorded on the alert
   * and does not undo the omission.
   */
  @Transactional
  public void handle(GenerateCaregiverAlertCommand command) {
    OmissionCase omissionCase = repository.findByIdForUpdate(command.omissionCaseId()).orElse(null);
    if (omissionCase == null
        || omissionCase.getStatus() != OmissionCaseStatus.OMITTED
        || !omissionCase.getAlerts().isEmpty()) {
      return;
    }
    CareAlert alert = omissionCase.addAlert(command.now());
    INotificationPort.NotificationResult result = notificationPort.sendCaregiverAlert(
        omissionCase.getOlderAdultId(),
        omissionCase.getMedicationName(),
        omissionCase.getScheduledAt());
    if (result.delivered()) {
      alert.markSent(command.now());
    } else {
      alert.markFailed(result.failureReason());
    }
    repository.save(omissionCase);
    eventPublisher.publish(new CaregiverAlertGenerated(
        omissionCase.getId(),
        omissionCase.getIntakeId(),
        omissionCase.getOlderAdultId(),
        command.now()));
  }
}
