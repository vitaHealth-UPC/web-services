package com.tata.familymonitoring.application.internal.commandservices;

import com.tata.familymonitoring.domain.model.commands.ResolveLowStockCommand;
import com.tata.familymonitoring.domain.ports.IMedicationLookupPort;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResolveLowStockCommandHandler {

  private final IFamilyMonitorRepository repository;
  private final IMedicationLookupPort medications;

  public ResolveLowStockCommandHandler(IFamilyMonitorRepository repository, IMedicationLookupPort medications) {
    this.repository = repository;
    this.medications = medications;
  }

  /** A replenishment clears the notice only when it leaves the stock above the threshold. */
  @Transactional
  public void handle(ResolveLowStockCommand command) {
    medications.findMedication(command.medicationId())
        .flatMap(medication -> repository.findByOlderAdultId(medication.olderAdultId()))
        .ifPresent(monitor -> {
          monitor.resolveLowStock(command.medicationId(), command.remainingStock());
          repository.save(monitor);
        });
  }
}
