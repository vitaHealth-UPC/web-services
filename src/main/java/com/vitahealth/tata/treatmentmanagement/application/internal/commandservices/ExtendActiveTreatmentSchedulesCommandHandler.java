package com.vitahealth.tata.treatmentmanagement.application.internal.commandservices;

import com.vitahealth.tata.treatmentmanagement.application.result.ScheduleHorizonExtensionResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ExtendTreatmentScheduleCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Coordinates independently committed extensions without retaining batch-wide write locks. */
@Service
public class ExtendActiveTreatmentSchedulesCommandHandler {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExtendActiveTreatmentSchedulesCommandHandler.class);
  private final TreatmentRepository treatments;
  private final ExtendTreatmentScheduleCommandHandler extensions;

  /** Injects candidate discovery and the proxied per-treatment transaction boundary. */
  public ExtendActiveTreatmentSchedulesCommandHandler(
      TreatmentRepository treatments, ExtendTreatmentScheduleCommandHandler extensions) {
    this.treatments = treatments;
    this.extensions = extensions;
  }

  /**
   * Continues after a rolled-back candidate and reports every outcome without claiming full
   * success.
   */
  public ScheduleHorizonExtensionResult handle() {
    int published = 0, skipped = 0, failed = 0;
    String afterId = "";
    while (true) {
      var candidates = treatments.findIdsByStatusAfter(TreatmentStatus.ACTIVE, afterId, 100);
      if (candidates.isEmpty()) break;
      for (var id : candidates) {
        try {
          if (extensions.handle(new ExtendTreatmentScheduleCommand(id))) published++;
          else skipped++;
        } catch (RuntimeException failure) {
          failed++;
          LOGGER.error("Schedule horizon extension failed for treatment {}", id, failure);
        }
      }
      afterId = candidates.getLast();
    }
    return new ScheduleHorizonExtensionResult(published, skipped, failed);
  }
}
