package com.vitahealth.tata.carelink.interfaces.rest.transform;

import com.vitahealth.tata.carelink.application.result.OlderAdultProfileResult;
import com.vitahealth.tata.carelink.interfaces.rest.resources.OlderAdultProfileResource;

/** Projects application profile evidence into the existing REST resource. */
public final class OlderAdultProfileResourceFromResultAssembler {
  private OlderAdultProfileResourceFromResultAssembler() {}

  /** Projects application profile evidence into the existing REST resource. */
  public static OlderAdultProfileResource toResource(OlderAdultProfileResult result) {
    return new OlderAdultProfileResource(
        result.id(),
        result.registeredByCaregiverId(),
        result.fullName(),
        result.birthDate(),
        result.emergencyContactName(),
        result.emergencyContactRelationship(),
        result.emergencyContactPhone(),
        result.createdAt());
  }
}
