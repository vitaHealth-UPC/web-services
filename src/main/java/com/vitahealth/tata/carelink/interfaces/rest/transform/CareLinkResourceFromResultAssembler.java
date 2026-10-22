package com.vitahealth.tata.carelink.interfaces.rest.transform;

import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.interfaces.rest.resources.CareLinkResource;

/** Projects authorized relationship evidence into the existing REST resource. */
public final class CareLinkResourceFromResultAssembler {
  private CareLinkResourceFromResultAssembler() {}

  /** Projects authorized relationship evidence into the existing REST resource. */
  public static CareLinkResource toResource(CareLinkResult result) {
    return new CareLinkResource(
        result.id(),
        result.caregiverId(),
        result.olderAdultId(),
        result.status(),
        result.linkingCode(),
        result.codeExpiresAt(),
        result.codeUsedAt(),
        result.consentGranted(),
        result.consentRecordedAt(),
        result.confirmedAt());
  }
}
