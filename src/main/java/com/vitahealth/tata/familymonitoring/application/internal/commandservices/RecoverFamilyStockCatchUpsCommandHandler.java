package com.vitahealth.tata.familymonitoring.application.internal.commandservices;

import com.vitahealth.tata.familymonitoring.domain.repositories.FamilyStockCatchUpRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Scans bounded work pages and isolates each medication step from other relationships. */
@Service
public class RecoverFamilyStockCatchUpsCommandHandler {
  private static final Logger LOG =
      LoggerFactory.getLogger(RecoverFamilyStockCatchUpsCommandHandler.class);
  private final FamilyStockCatchUpRepository work;
  private final ProcessFamilyStockCatchUpCommandHandler process;
  private final RetryFamilyStockCatchUpCommandHandler retry;
  private final Clock clock;

  /** Injects independent step/retry transaction boundaries and immutable recovery reads. */
  public RecoverFamilyStockCatchUpsCommandHandler(
      FamilyStockCatchUpRepository work,
      ProcessFamilyStockCatchUpCommandHandler process,
      RetryFamilyStockCatchUpCommandHandler retry,
      Clock clock) {
    this.work = work;
    this.process = process;
    this.retry = retry;
    this.clock = clock;
  }

  /** Limits total processing attempts and visits each eligible relationship once per run. */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void handle(int maximumSteps) {
    if (maximumSteps < 1 || maximumSteps > 1000)
      throw new IllegalArgumentException("invalid stock catch-up recovery limit");
    var cutoff = clock.instant();
    long cursor = 0;
    int attempted = 0;
    while (attempted < maximumSteps) {
      var page = work.findDueAfter(cutoff, cursor, Math.min(25, maximumSteps - attempted));
      if (page.isEmpty()) return;
      for (var request : page) {
        if (request.monitorId() <= cursor)
          throw new IllegalStateException("catch-up recovery cursor did not advance");
        cursor = request.monitorId();
        for (int step = 0; step < 10 && attempted < maximumSteps; step++) {
          var observed = work.find(request.monitorId());
          if (observed.isEmpty() || observed.get().nextAttemptAt().isAfter(clock.instant())) break;
          attempted++;
          try {
            if (!process.handle(request.monitorId())) break;
          } catch (RuntimeException failure) {
            LOG.warn(
                "Family stock catch-up remains pending: {}", failure.getClass().getSimpleName());
            try {
              retry.handle(observed.orElseThrow());
            } catch (RuntimeException retryFailure) {
              LOG.warn(
                  "Family stock retry evidence remains unchanged: {}",
                  retryFailure.getClass().getSimpleName());
            }
            break;
          }
        }
      }
    }
  }
}
