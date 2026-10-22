package com.vitahealth.tata.omissionescalation.domain.model.commands;

import java.time.Instant;

/** Evaluates all eligible work using one explicitly supplied cycle instant. */
public record EvaluateGracePeriodCommand(Instant now) {
  /** Requires the scheduler or caller to supply its clock instant. */
  public EvaluateGracePeriodCommand {
    if (now == null) throw new IllegalArgumentException("evaluation time is required");
  }
}
