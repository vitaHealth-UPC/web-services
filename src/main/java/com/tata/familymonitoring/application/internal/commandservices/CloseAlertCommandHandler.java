package com.tata.familymonitoring.application.internal.commandservices;

import com.tata.familymonitoring.domain.exceptions.FamilyMonitorNotFoundException;
import com.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.tata.familymonitoring.domain.model.commands.CloseAlertCommand;
import com.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseAlertCommandHandler {

  private final IFamilyMonitorRepository repository;

  public CloseAlertCommandHandler(IFamilyMonitorRepository repository) {
    this.repository = repository;
  }

  /** Closes the alert; it stops being pending but stays in the history. */
  @Transactional
  public AlertSummary handle(CloseAlertCommand command) {
    FamilyMonitor monitor = repository.findByOlderAdultId(command.olderAdultId())
        .orElseThrow(() -> new FamilyMonitorNotFoundException(command.olderAdultId()));
    monitor.closeAlert(command.alertId(), Instant.now());
    return repository.save(monitor).findAlert(command.alertId());
  }
}
