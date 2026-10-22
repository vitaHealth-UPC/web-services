package com.vitahealth.tata.familymonitoring.application.internal.commandservices;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.FamilyStockCatchUp;
import com.vitahealth.tata.familymonitoring.domain.repositories.FamilyStockCatchUpRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records bounded backoff independently from a failed stock projection transaction. */
@Service
public class RetryFamilyStockCatchUpCommandHandler {
  private final FamilyStockCatchUpRepository work;
  private final Clock clock;

  /** Injects conditional work storage and the application clock. */
  public RetryFamilyStockCatchUpCommandHandler(FamilyStockCatchUpRepository work, Clock clock) {
    this.work = work;
    this.clock = clock;
  }

  /** A stale failure never overwrites successfully advanced or completed work. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(FamilyStockCatchUp expected) {
    long delay = Math.min(3600L, 30L << Math.min(expected.attempts(), 7));
    var next = clock.instant().plusSeconds(delay);
    if (next.isBefore(expected.nextAttemptAt())) next = expected.nextAttemptAt();
    work.replace(expected, expected.retryAt(next));
  }
}
