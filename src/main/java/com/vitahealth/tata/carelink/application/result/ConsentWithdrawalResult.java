package com.vitahealth.tata.carelink.application.result;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Explicit outcome distinguishing first withdrawal from a previously withdrawn relationship. */
public record ConsentWithdrawalResult(String careLinkId, boolean changed) {
  /** Requires the relationship whose withdrawal workflow completed. */
  public ConsentWithdrawalResult {
    careLinkId = requireText(careLinkId, "careLinkId");
  }
}
