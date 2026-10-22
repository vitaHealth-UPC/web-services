package com.vitahealth.tata.familymonitoring.domain.model.valueobjects;

/** Weekly adherence indicators calculated by Adherence Analytics. */
public record AdherenceSnapshot(int confirmedIntakes, int totalIntakes) {
  /** Accepts no-data counts but never fabricates or clamps inconsistent source metrics. */
  public AdherenceSnapshot {
    if (confirmedIntakes < 0 || totalIntakes < 0 || confirmedIntakes > totalIntakes)
      throw new IllegalArgumentException("confirmed intakes must fit the nonnegative total");
  }
}
