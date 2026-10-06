package com.tata.omissionescalation.domain.factories;

import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import java.time.Instant;

public final class OmissionCaseFactory {

  private OmissionCaseFactory() {
  }

  public static OmissionCase createPending(
      String intakeId,
      String olderAdultId,
      String medicationName,
      Instant scheduledAt,
      GracePeriod gracePeriod) {
    return new OmissionCase(intakeId, olderAdultId, medicationName, scheduledAt, gracePeriod);
  }
}
