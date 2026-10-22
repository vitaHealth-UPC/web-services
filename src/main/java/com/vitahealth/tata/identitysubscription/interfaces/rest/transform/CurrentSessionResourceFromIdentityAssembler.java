package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.CurrentSessionResource;

/** Separates the authenticated application identity from its public HTTP representation. */
public final class CurrentSessionResourceFromIdentityAssembler {
  private CurrentSessionResourceFromIdentityAssembler() {}

  /** Preserves the existing subject, role, expiration and relationship scope response fields. */
  public static CurrentSessionResource toResource(AuthenticatedSession session) {
    return new CurrentSessionResource(
        session.subjectId(), session.role().name(), session.expiresAt(), session.careLinkId());
  }
}
