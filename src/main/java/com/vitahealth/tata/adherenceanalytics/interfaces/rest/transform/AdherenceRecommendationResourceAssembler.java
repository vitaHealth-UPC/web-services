package com.vitahealth.tata.adherenceanalytics.interfaces.rest.transform;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceInsight;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources.AdherenceRecommendationResource;

/** Maps source evidence into its explicit REST representation. */
public final class AdherenceRecommendationResourceAssembler {
  private AdherenceRecommendationResourceAssembler() {}

  /** Preserves recorded evidence without recalculating domain state. */
  public static AdherenceRecommendationResource from(AdherenceInsight insight) {
    return new AdherenceRecommendationResource(
        insight.medicationId(), insight.code(), insight.evidenceDays());
  }
}
