package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.PinSessionResource;

/** Projects the authenticated adult identity and issued credential into the PIN response. */
public final class PinSessionResourceFromResultAssembler {
  private PinSessionResourceFromResultAssembler() {}

  /** Uses the authenticated result's subject rather than replaying the request selector. */
  public static PinSessionResource toResource(SessionResult result) {
    return new PinSessionResource(result.accountId(), result.accessToken(), result.expiresAt());
  }
}
