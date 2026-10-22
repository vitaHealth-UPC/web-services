package com.vitahealth.tata.familymonitoring.application.internal.commandservices;

import com.vitahealth.tata.familymonitoring.application.commandservices.CloseAlertCommandService;
import com.vitahealth.tata.familymonitoring.application.internal.queryservices.FamilyMonitorSelectionService;
import com.vitahealth.tata.familymonitoring.application.result.AlertResult;
import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.model.commands.CloseAlertCommand;
import com.vitahealth.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operates on alert evidence within the selected caregiver-adult follow-up. */
@Service
public class CloseAlertCommandHandler implements CloseAlertCommandService {

  private final IFamilyMonitorRepository repository;
  private final FamilyMonitorSelectionService selection;
  private final Clock clock;

  /** Injects the context-owned follow-up repository. */
  public CloseAlertCommandHandler(
      IFamilyMonitorRepository repository, Clock clock, FamilyMonitorSelectionService selection) {
    this.repository = repository;
    this.selection = selection;
    this.clock = clock;
  }

  /** Closes the alert; it stops being pending but stays in the history. */
  @Override
  @Transactional
  public AlertResult handle(CloseAlertCommand command) {
    FamilyMonitor monitor =
        selection.requireCurrentForUpdate(command.olderAdultId(), command.caregiverId());
    monitor.closeAlert(command.alertId(), clock.instant());
    return AlertResult.from(repository.save(monitor).findAlert(command.alertId()));
  }
}
