package com.vitahealth.tata.inventoryreplenishment.infrastructure.scheduling;

import com.vitahealth.tata.inventoryreplenishment.application.internal.commandservices.RecoverInventoryConsumptionsCommandHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically retries persisted decrement intent without retaining a batch transaction. */
@Component
public class InventoryConsumptionRecoveryScheduler {
  private final RecoverInventoryConsumptionsCommandHandler recovery;
  private final int maximumItems;

  /** Validates the configured bound before scheduling recovery. */
  public InventoryConsumptionRecoveryScheduler(
      RecoverInventoryConsumptionsCommandHandler recovery,
      @Value("${tata.inventory.recovery-maximum-items:100}") int maximumItems) {
    if (maximumItems < 1 || maximumItems > 1000)
      throw new IllegalArgumentException("invalid consumption recovery limit");
    this.recovery = recovery;
    this.maximumItems = maximumItems;
  }

  /** Processes bounded due intent; failures leave durable requests available to subsequent runs. */
  @Scheduled(
      fixedDelayString = "${tata.inventory.recovery-delay:30000}",
      initialDelayString = "${tata.inventory.recovery-initial-delay:30000}")
  public void recover() {
    recovery.handle(maximumItems);
  }
}
