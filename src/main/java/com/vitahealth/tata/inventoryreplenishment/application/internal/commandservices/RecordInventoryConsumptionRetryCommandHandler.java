package com.vitahealth.tata.inventoryreplenishment.application.internal.commandservices;

import com.vitahealth.tata.inventoryreplenishment.domain.model.valueobjects.PendingInventoryConsumption;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryConsumptionRequestRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records retry metadata only after the failed stock-processing transaction has rolled back. */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class RecordInventoryConsumptionRetryCommandHandler {
  private final InventoryConsumptionRequestRepository requests;

  /** Injects conditional retry persistence that cannot recreate completed intent. */
  public RecordInventoryConsumptionRetryCommandHandler(
      InventoryConsumptionRequestRepository requests) {
    this.requests = requests;
  }

  /** Advances the expected attempt in an independent transaction. */
  public void handle(PendingInventoryConsumption expected, Instant nextAttemptAt) {
    requests.recordRetry(expected, nextAttemptAt);
  }
}
