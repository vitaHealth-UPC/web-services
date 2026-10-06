package com.tata.omissionescalation.infrastructure.scheduling;

import com.tata.omissionescalation.application.internal.commandservices.EvaluateGracePeriodCommandHandler;
import com.tata.omissionescalation.domain.model.commands.EvaluateGracePeriodCommand;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OmissionEvaluationScheduler {

  private final EvaluateGracePeriodCommandHandler evaluateHandler;

  public OmissionEvaluationScheduler(EvaluateGracePeriodCommandHandler evaluateHandler) {
    this.evaluateHandler = evaluateHandler;
  }

  @Scheduled(
      fixedDelayString = "${tata.omission.evaluation-interval:PT1M}",
      initialDelayString = "${tata.omission.evaluation-initial-delay:PT1M}")
  public void evaluatePendingCases() {
    evaluateHandler.handle(new EvaluateGracePeriodCommand(Instant.now()));
  }
}
