package com.vitahealth.tata.adherenceanalytics.interfaces.rest.transform;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceOmissionPattern;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources.AdherencePatternResource;

/** Maps source evidence into its explicit REST representation. */
public final class AdherencePatternResourceAssembler {
  private AdherencePatternResourceAssembler() {}

  /** Preserves recorded evidence without recalculating domain state. */
  public static AdherencePatternResource from(AdherenceOmissionPattern pattern) {
    return new AdherencePatternResource(
        pattern.medicationId(), pattern.omissionDays(), pattern.firstDay(), pattern.lastDay());
  }
}
