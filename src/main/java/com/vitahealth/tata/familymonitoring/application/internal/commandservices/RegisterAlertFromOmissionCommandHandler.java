package com.vitahealth.tata.familymonitoring.application.internal.commandservices;

import com.vitahealth.tata.familymonitoring.domain.model.commands.RegisterAlertFromOmissionCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Projects one omission to every currently consented relationship follow-up. */
@Service
public class RegisterAlertFromOmissionCommandHandler {
  private final AuthorizedFamilyMonitorProjectionService projections;

  /** Injects reserved, authorized relationship projection. */
  public RegisterAlertFromOmissionCommandHandler(
      AuthorizedFamilyMonitorProjectionService projections) {
    this.projections = projections;
  }

  /** Preserves source occurrence time and rollback across all eligible relationship projections. */
  @Transactional
  public void handle(RegisterAlertFromOmissionCommand command) {
    projections.apply(
        command.olderAdultId(),
        monitor ->
            monitor.addAlert(
                command.intakeId(),
                command.medicationName(),
                command.scheduledAt(),
                command.reason(),
                command.occurredAt()));
  }
}
