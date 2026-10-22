package com.vitahealth.tata.carelink.interfaces.rest.transform;

import com.vitahealth.tata.carelink.application.result.AuthenticatedCareLinkResult;
import com.vitahealth.tata.carelink.interfaces.rest.resources.CareLinkResource;

/** Projects relationship and optional issued credential into the existing REST resource. */
public final class AuthenticatedCareLinkResourceFromResultAssembler {
  private AuthenticatedCareLinkResourceFromResultAssembler() {}

  /** Projects relationship and optional issued credential into the existing REST resource. */
  public static CareLinkResource toResource(AuthenticatedCareLinkResult result) {
    var l = result.link();
    return new CareLinkResource(
        l.id(),
        l.caregiverId(),
        l.olderAdultId(),
        l.status(),
        l.linkingCode(),
        l.codeExpiresAt(),
        l.codeUsedAt(),
        l.consentGranted(),
        l.consentRecordedAt(),
        l.confirmedAt(),
        result.accessToken(),
        result.expiresAt());
  }
}
