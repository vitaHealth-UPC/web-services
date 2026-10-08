package com.tata.familymonitoring.application.internal.commandservices;

import com.tata.familymonitoring.domain.exceptions.FamilyMonitorNotFoundException;
import com.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.tata.familymonitoring.domain.model.commands.MarkAlertAttendedCommand;
import com.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarkAlertAttendedCommandHandler implements com.tata.familymonitoring.application.commandservices.MarkAlertAttendedCommandService {

  private final IFamilyMonitorRepository repository;

  public MarkAlertAttendedCommandHandler(IFamilyMonitorRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public AlertSummary handle(MarkAlertAttendedCommand command) {
    FamilyMonitor monitor = repository.findByOlderAdultId(command.olderAdultId())
        .orElseThrow(() -> new FamilyMonitorNotFoundException(command.olderAdultId()));
    monitor.markAlertAttended(command.alertId());
    return repository.save(monitor).findAlert(command.alertId());
  }
}
