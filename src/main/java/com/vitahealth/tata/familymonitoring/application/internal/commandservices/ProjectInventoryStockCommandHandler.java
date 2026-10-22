package com.vitahealth.tata.familymonitoring.application.internal.commandservices;

import com.vitahealth.tata.familymonitoring.domain.model.commands.ProjectInventoryStockCommand;
import com.vitahealth.tata.familymonitoring.domain.ports.MedicationLookupPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Resolves medication ownership and projects revisioned stock within its source transaction. */
@Service
public class ProjectInventoryStockCommandHandler {
  private final AuthorizedFamilyMonitorProjectionService projections;
  private final MedicationLookupPort medications;

  /** Injects the consent-aware projection boundary and public medication lookup. */
  public ProjectInventoryStockCommandHandler(
      AuthorizedFamilyMonitorProjectionService projections, MedicationLookupPort medications) {
    this.projections = projections;
    this.medications = medications;
  }

  /** Uses actual medication ownership; unknown medication evidence cannot select another adult. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void handle(ProjectInventoryStockCommand command) {
    var position = command.position();
    medications
        .findMedication(position.medicationId())
        .ifPresent(
            medication ->
                projections.applyIfChanged(
                    medication.olderAdultId(),
                    monitor -> monitor.projectInventoryStock(position, medication.name())));
  }
}
