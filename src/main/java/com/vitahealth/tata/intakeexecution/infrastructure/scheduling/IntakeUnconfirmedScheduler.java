package com.vitahealth.tata.intakeexecution.infrastructure.scheduling;

import com.vitahealth.tata.intakeexecution.application.internal.commandservices.ReportUnconfirmedIntakesCommandHandler;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ReportUnconfirmedIntakesCommand;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Uses the application clock to submit bounded evaluations of unresolved intake evidence. */
@Component
public class IntakeUnconfirmedScheduler {
  private final ReportUnconfirmedIntakesCommandHandler handler;
  private final Duration unconfirmedAfter;
  private final Clock clock;

  /**
   * Requires configured timing and the shared application clock, without a system-clock fallback.
   */
  public IntakeUnconfirmedScheduler(
      ReportUnconfirmedIntakesCommandHandler handler,
      @Value("${tata.intake.unconfirmed-after:PT15M}") Duration unconfirmedAfter,
      Clock clock) {
    this.handler = Objects.requireNonNull(handler, "handler");
    this.unconfirmedAfter = Objects.requireNonNull(unconfirmedAfter, "unconfirmedAfter");
    this.clock = Objects.requireNonNull(clock, "clock");
    if (unconfirmedAfter.isNegative())
      throw new IllegalArgumentException("unconfirmedAfter cannot be negative");
  }

  /** Captures one instant for the cutoff and reporting timestamp of this evaluation. */
  @Scheduled(
      fixedDelayString = "${tata.intake.unconfirmed-evaluation-interval:PT1M}",
      initialDelayString = "${tata.intake.unconfirmed-evaluation-initial-delay:PT1M}")
  public void reportUnconfirmedIntakes() {
    var now = clock.instant();
    handler.handle(new ReportUnconfirmedIntakesCommand(now.minus(unconfirmedAfter), now));
  }
}
