package com.tata.familymonitoring.application.internal.commandservices;

import com.tata.familymonitoring.domain.model.commands.RegisterAdherenceInsightCommand;
import com.tata.familymonitoring.domain.ports.IMedicationLookupPort;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterAdherenceInsightCommandHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterAdherenceInsightCommandHandler.class);

  private final IFamilyMonitorRepository repository;
  private final IMedicationLookupPort medications;

  public RegisterAdherenceInsightCommandHandler(IFamilyMonitorRepository repository, IMedicationLookupPort medications) {
    this.repository = repository;
    this.medications = medications;
  }

  /** Keeps the pattern as an insight of the caregiver. Reporting the same pattern again changes nothing. */
  @Transactional
  public void handle(RegisterAdherenceInsightCommand command) {
    var monitor = repository.findByOlderAdultId(command.olderAdultId()).orElse(null);
    if (monitor == null) {
      LOGGER.warn("No family monitor for older adult {}; adherence insight was not registered", command.olderAdultId());
      return;
    }
    var medicationName = medications.findMedication(command.medicationId())
        .map(IMedicationLookupPort.MedicationInfo::name)
        .orElse("Unknown medication");
    monitor.registerInsight(
        command.medicationId(),
        medicationName,
        command.omissionDays(),
        command.firstDay(),
        command.lastDay(),
        command.detectedAt());
    repository.save(monitor);
  }
}
