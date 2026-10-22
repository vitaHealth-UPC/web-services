package com.vitahealth.tata.treatmentmanagement.application.internal.authorization;

import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.CareLinkVerificationPort;
import org.springframework.stereotype.Component;

/** Requires current consented access before caregiver treatment reads or mutations. */
@Component
public class TreatmentCareLinkGuard {
  private final CareLinkVerificationPort links;

  /** Injects the context authorization boundary without accessing Care Link persistence. */
  public TreatmentCareLinkGuard(CareLinkVerificationPort links) {
    this.links = links;
  }

  /** Rejects unavailable relationships while propagating technical authorization failures. */
  public void requireAuthorized(String caregiverId, String olderAdultId) {
    if (caregiverId == null
        || caregiverId.isBlank()
        || olderAdultId == null
        || olderAdultId.isBlank()
        || !links.isAuthorized(caregiverId, olderAdultId)) {
      throw new TreatmentApplicationException(
          TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED,
          "an active care link is required for this older adult");
    }
  }
}
