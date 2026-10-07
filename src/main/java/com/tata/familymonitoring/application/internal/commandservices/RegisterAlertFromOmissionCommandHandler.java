package com.tata.familymonitoring.application.internal.commandservices;

import com.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.tata.familymonitoring.domain.model.commands.RegisterAlertFromOmissionCommand;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterAlertFromOmissionCommandHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(RegisterAlertFromOmissionCommandHandler.class);

  private final IFamilyMonitorRepository repository;

  public RegisterAlertFromOmissionCommandHandler(IFamilyMonitorRepository repository) {
    this.repository = repository;
  }

  /** Adds the alert to the monitor of the older adult. Without a monitor there is nobody to notify. */
  @Transactional
  public void handle(RegisterAlertFromOmissionCommand command) {
    FamilyMonitor monitor = repository.findByOlderAdultId(command.olderAdultId()).orElse(null);
    if (monitor == null) {
      LOGGER.warn("No family monitor for older adult {}; alert for intake {} was not registered",
          command.olderAdultId(), command.intakeId());
      return;
    }
    monitor.addAlert(
        command.intakeId(),
        command.medicationName(),
        command.scheduledAt(),
        command.reason(),
        Instant.now());
    repository.save(monitor);
  }
}
