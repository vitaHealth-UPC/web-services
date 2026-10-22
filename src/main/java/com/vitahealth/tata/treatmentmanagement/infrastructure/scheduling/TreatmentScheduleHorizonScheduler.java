package com.vitahealth.tata.treatmentmanagement.infrastructure.scheduling;

import com.vitahealth.tata.treatmentmanagement.application.internal.commandservices.ExtendActiveTreatmentSchedulesCommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the Intake Execution generation window rolling by re-emitting active treatment schedules.
 */
@Component
public class TreatmentScheduleHorizonScheduler {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(TreatmentScheduleHorizonScheduler.class);

  private final ExtendActiveTreatmentSchedulesCommandHandler extendActiveSchedules;

  /** Injects the batch coordinator. */
  public TreatmentScheduleHorizonScheduler(
      ExtendActiveTreatmentSchedulesCommandHandler extendActiveSchedules) {
    this.extendActiveSchedules = extendActiveSchedules;
  }

  /** Reports committed, skipped and failed candidates independently. */
  @Scheduled(
      cron = "${tata.treatment.horizon-extension-cron:0 15 0 * * *}",
      zone = "${tata.treatment.horizon-extension-zone:UTC}")
  public void extendHorizon() {
    try {
      var result = extendActiveSchedules.handle();
      LOGGER.info(
          "Intake schedule horizon: {} extended, {} skipped, {} failed",
          result.published(),
          result.skipped(),
          result.failed());
    } catch (RuntimeException exception) {
      LOGGER.error("Failed to extend intake schedule horizon", exception);
    }
  }
}
