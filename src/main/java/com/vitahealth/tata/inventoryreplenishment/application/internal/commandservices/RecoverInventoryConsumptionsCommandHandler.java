package com.vitahealth.tata.inventoryreplenishment.application.internal.commandservices;

import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RequestInventoryConsumptionCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.valueobjects.PendingInventoryConsumption;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryConsumptionRequestRepository;
import java.time.Clock;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates bounded recovery while keeping each decrement and retry in its own transaction. */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class RecoverInventoryConsumptionsCommandHandler {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(RecoverInventoryConsumptionsCommandHandler.class);
  private static final int PAGE_SIZE = 25;
  private final InventoryConsumptionRequestRepository requests;
  private final ProcessInventoryConsumptionCommandHandler process;
  private final RecordInventoryConsumptionRetryCommandHandler retries;
  private final Clock clock;

  /** Injects durable scans, isolated processing, isolated retries and the application clock. */
  public RecoverInventoryConsumptionsCommandHandler(
      InventoryConsumptionRequestRepository requests,
      ProcessInventoryConsumptionCommandHandler process,
      RecordInventoryConsumptionRetryCommandHandler retries,
      Clock clock) {
    this.requests = requests;
    this.process = process;
    this.retries = retries;
    this.clock = clock;
  }

  /** Attempts at most the requested number of due items using one fixed scan cutoff. */
  public void handle(int maximumItems) {
    if (maximumItems < 1 || maximumItems > 1000)
      throw new IllegalArgumentException("invalid consumption recovery limit");
    var cutoff = clock.instant();
    PendingInventoryConsumption cursor = null;
    int attempted = 0;
    while (attempted < maximumItems) {
      var page =
          requests.findDueAfter(cutoff, cursor, Math.min(PAGE_SIZE, maximumItems - attempted));
      if (page.isEmpty()) return;
      for (var request : page) {
        attemptRequest(request);
        cursor = request;
        attempted++;
      }
    }
  }

  /**
   * Accelerates one committed event without bypassing the durable request or source verification.
   */
  public void attempt(RequestInventoryConsumptionCommand command) {
    Objects.requireNonNull(command, "command");
    requests
        .findByIntakeId(command.intakeId())
        .ifPresent(
            request -> {
              if (!request.matches(command))
                throw new IllegalStateException(
                    "inventory consumption acceleration evidence does not match");
              attemptRequest(request);
            });
  }

  private void attemptRequest(PendingInventoryConsumption request) {
    try {
      process.handle(
          new RequestInventoryConsumptionCommand(
              request.intakeId(),
              request.medicationId(),
              request.olderAdultId(),
              request.confirmedAt()));
    } catch (RuntimeException failure) {
      LOGGER.warn("Inventory consumption remains pending: {}", failure.getClass().getSimpleName());
      long delaySeconds = Math.min(3600L, 30L << Math.min(request.attempts(), 7));
      try {
        var next = clock.instant().plusSeconds(delaySeconds);
        if (next.isBefore(request.nextAttemptAt())) next = request.nextAttemptAt();
        retries.handle(request, next);
      } catch (RuntimeException retryFailure) {
        LOGGER.warn(
            "Inventory retry metadata could not advance: {}",
            retryFailure.getClass().getSimpleName());
      }
    }
  }
}
