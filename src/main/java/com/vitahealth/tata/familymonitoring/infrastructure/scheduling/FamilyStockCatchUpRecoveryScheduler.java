package com.vitahealth.tata.familymonitoring.infrastructure.scheduling;

import com.vitahealth.tata.familymonitoring.application.internal.commandservices.RecoverFamilyStockCatchUpsCommandHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Resumes committed initial stock work after failures or application restart. */
@Component
public class FamilyStockCatchUpRecoveryScheduler {
  private final RecoverFamilyStockCatchUpsCommandHandler recovery;
  private final int maximumSteps;

  /** Validates the configured transaction attempt bound. */
  public FamilyStockCatchUpRecoveryScheduler(
      RecoverFamilyStockCatchUpsCommandHandler recovery,
      @Value("${tata.family.stock-catchup-maximum-steps:100}") int maximumSteps) {
    if (maximumSteps < 1 || maximumSteps > 1000)
      throw new IllegalArgumentException("invalid stock catch-up recovery limit");
    this.recovery = recovery;
    this.maximumSteps = maximumSteps;
  }

  /** Recovers bounded work outside a batch transaction. */
  @Scheduled(
      fixedDelayString = "${tata.family.stock-catchup-delay:30000}",
      initialDelayString = "${tata.family.stock-catchup-initial-delay:30000}")
  public void recover() {
    recovery.handle(maximumSteps);
  }
}
