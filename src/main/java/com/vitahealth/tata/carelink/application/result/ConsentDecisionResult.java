package com.vitahealth.tata.carelink.application.result;

import java.util.Objects;

/** Records whether a locked consent command changed state or returned an existing decision. */
public record ConsentDecisionResult(CareLinkResult link, boolean changed) {
  /** Requires the relationship evidence returned by the decision transaction. */
  public ConsentDecisionResult {
    Objects.requireNonNull(link, "link");
  }

  /** Omits linking credentials and personal evidence from diagnostic output. */
  @Override
  public String toString() {
    return "ConsentDecisionResult[careLinkId=" + link.id() + ", changed=" + changed + "]";
  }
}
