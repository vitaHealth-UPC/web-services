package com.vitahealth.tata.inventoryreplenishment.infrastructure.events;

import com.vitahealth.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import com.vitahealth.tata.inventoryreplenishment.application.internal.commandservices.RecoverInventoryConsumptionsCommandHandler;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RequestInventoryConsumptionCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Accelerates committed requests; persistent recovery remains available if this callback fails. */
@Component("inventoryIntakeConfirmedEventListener")
public class IntakeConfirmedEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(IntakeConfirmedEventListener.class);
  private final RecoverInventoryConsumptionsCommandHandler recovery;

  /** Injects the coordinator that isolates processing and retry transactions. */
  public IntakeConfirmedEventListener(RecoverInventoryConsumptionsCommandHandler recovery) {
    this.recovery = recovery;
  }

  /**
   * Attempts delivery only after source commit; never turns a committed confirmation into failure.
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(IntakeConfirmed event) {
    try {
      recovery.attempt(
          new RequestInventoryConsumptionCommand(
              event.intakeId(), event.medicationId(), event.olderAdultId(), event.confirmedAt()));
    } catch (RuntimeException failure) {
      LOGGER.warn(
          "Inventory consumption acceleration failed: {}", failure.getClass().getSimpleName());
    }
  }
}
