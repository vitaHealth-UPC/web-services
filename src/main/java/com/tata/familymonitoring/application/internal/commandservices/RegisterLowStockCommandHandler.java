package com.tata.familymonitoring.application.internal.commandservices;

import com.tata.familymonitoring.domain.model.commands.RegisterLowStockCommand;
import com.tata.familymonitoring.domain.ports.IMedicationLookupPort;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterLowStockCommandHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterLowStockCommandHandler.class);

  private final IFamilyMonitorRepository repository;
  private final IMedicationLookupPort medications;

  public RegisterLowStockCommandHandler(IFamilyMonitorRepository repository, IMedicationLookupPort medications) {
    this.repository = repository;
    this.medications = medications;
  }

  /** Finds the older adult through the medication and flags the low stock on their follow-up. */
  @Transactional
  public void handle(RegisterLowStockCommand command) {
    var medication = medications.findMedication(command.medicationId()).orElse(null);
    if (medication == null) {
      LOGGER.warn("Low stock of unknown medication {} was ignored", command.medicationId());
      return;
    }
    var monitor = repository.findByOlderAdultId(medication.olderAdultId()).orElse(null);
    if (monitor == null) {
      LOGGER.warn("No family monitor for older adult {}; low stock of {} was not registered",
          medication.olderAdultId(), command.medicationId());
      return;
    }
    monitor.registerLowStock(
        command.medicationId(),
        medication.name(),
        command.remainingStock(),
        command.replenishmentThreshold(),
        command.detectedAt());
    repository.save(monitor);
  }
}
