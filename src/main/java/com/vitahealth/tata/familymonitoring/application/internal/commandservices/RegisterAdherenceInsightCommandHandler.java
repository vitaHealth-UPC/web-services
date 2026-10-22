package com.vitahealth.tata.familymonitoring.application.internal.commandservices;

import com.vitahealth.tata.familymonitoring.domain.model.commands.RegisterAdherenceInsightCommand;
import com.vitahealth.tata.familymonitoring.domain.ports.MedicationLookupPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Projects source adherence patterns only for the actual medication owner and consented links. */
@Service
public class RegisterAdherenceInsightCommandHandler {
  private final AuthorizedFamilyMonitorProjectionService projections;
  private final MedicationLookupPort medications;

  /** Injects authorized follow-up projection and public medication ownership lookup. */
  public RegisterAdherenceInsightCommandHandler(
      AuthorizedFamilyMonitorProjectionService projections, MedicationLookupPort medications) {
    this.projections = projections;
    this.medications = medications;
  }

  /** Retains source period and occurrence evidence without fabricating missing medication names. */
  @Transactional
  public void handle(RegisterAdherenceInsightCommand command) {
    medications
        .findMedication(command.medicationId())
        .filter(medication -> medication.olderAdultId().equals(command.olderAdultId()))
        .ifPresent(
            medication ->
                projections.apply(
                    command.olderAdultId(),
                    monitor ->
                        monitor.registerInsight(
                            command.medicationId(),
                            medication.name(),
                            command.omissionDays(),
                            command.firstDay(),
                            command.lastDay(),
                            command.detectedAt())));
  }
}
