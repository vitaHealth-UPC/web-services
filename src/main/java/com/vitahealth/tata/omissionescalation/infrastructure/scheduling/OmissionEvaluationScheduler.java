package com.vitahealth.tata.omissionescalation.infrastructure.scheduling;

import com.vitahealth.tata.omissionescalation.application.internal.commandservices.EvaluateGracePeriodCommandHandler;
import com.vitahealth.tata.omissionescalation.domain.model.commands.EvaluateGracePeriodCommand;
import java.time.Clock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Triggers omission evaluation with one instant from the configured application clock. */
@Component
public class OmissionEvaluationScheduler {

  private final EvaluateGracePeriodCommandHandler evaluateHandler;
  private final Clock clock;

  /** Injects the clinical evaluation boundary and the application occurrence clock. */
  public OmissionEvaluationScheduler(
      EvaluateGracePeriodCommandHandler evaluateHandler, Clock clock) {
    this.evaluateHandler = evaluateHandler;
    this.clock = clock;
  }

  /** Evaluates pending cases using one occurrence time for the scheduled cycle. */
  @Scheduled(
      fixedDelayString = "${tata.omission.evaluation-interval:PT1M}",
      initialDelayString = "${tata.omission.evaluation-initial-delay:PT1M}")
  public void evaluatePendingCases() {
    evaluateHandler.handle(new EvaluateGracePeriodCommand(clock.instant()));
  }
}
