package com.vitahealth.tata.familymonitoring.application.internal.commandservices;

import com.vitahealth.tata.familymonitoring.application.commandservices.MarkAlertAttendedCommandService;
import com.vitahealth.tata.familymonitoring.application.internal.queryservices.AlertResultProjection;
import com.vitahealth.tata.familymonitoring.application.internal.queryservices.FamilyMonitorSelectionService;
import com.vitahealth.tata.familymonitoring.application.result.AlertResult;
import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.model.commands.MarkAlertAttendedCommand;
import com.vitahealth.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operates on alert evidence within the selected caregiver-adult follow-up. */
@Service
public class MarkAlertAttendedCommandHandler implements MarkAlertAttendedCommandService {

  private final IFamilyMonitorRepository repository;
  private final FamilyMonitorSelectionService selection;
  private final AlertResultProjection projection;

  /** Injects the context-owned follow-up repository. */
  public MarkAlertAttendedCommandHandler(
      IFamilyMonitorRepository repository,
      FamilyMonitorSelectionService selection,
      AlertResultProjection projection) {
    this.repository = repository;
    this.selection = selection;
    this.projection = projection;
  }

  /** Records attendance on the requested alert without modifying another caregiver's history. */
  @Override
  @Transactional
  public AlertResult handle(MarkAlertAttendedCommand command) {
    FamilyMonitor monitor =
        selection.requireCurrentForUpdate(command.olderAdultId(), command.caregiverId());
    monitor.markAlertAttended(command.alertId());
    return projection.project(
        command.olderAdultId(), repository.save(monitor).findAlert(command.alertId()));
  }
}
