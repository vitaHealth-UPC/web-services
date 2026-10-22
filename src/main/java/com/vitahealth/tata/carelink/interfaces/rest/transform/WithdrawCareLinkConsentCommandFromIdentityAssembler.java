package com.vitahealth.tata.carelink.interfaces.rest.transform;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.domain.model.commands.WithdrawCareLinkConsentCommand;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;

/** Builds withdrawal commands exclusively from the authenticated adult and relationship path. */
public final class WithdrawCareLinkConsentCommandFromIdentityAssembler {
  private WithdrawCareLinkConsentCommandFromIdentityAssembler() {}

  /** Rejects non-adult identities instead of accepting an actor from untrusted request data. */
  public static WithdrawCareLinkConsentCommand toCommand(
      String careLinkId, AuthenticatedSession session) {
    if (session == null || session.role() != Role.OLDER_ADULT) {
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.CONSENT_REQUIRED,
          "only the linked adult can withdraw consent");
    }
    return new WithdrawCareLinkConsentCommand(careLinkId, session.subjectId());
  }
}
