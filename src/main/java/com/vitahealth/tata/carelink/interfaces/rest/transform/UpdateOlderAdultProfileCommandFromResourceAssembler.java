package com.vitahealth.tata.carelink.interfaces.rest.transform;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.domain.model.commands.UpdateOlderAdultProfileCommand;
import com.vitahealth.tata.carelink.interfaces.rest.resources.UpdateOlderAdultProfileResource;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;

/** Builds profile changes with the verified caregiver rather than an actor supplied in JSON. */
public final class UpdateOlderAdultProfileCommandFromResourceAssembler {
  private UpdateOlderAdultProfileCommandFromResourceAssembler() {}

  /** Requires caregiver identity before delegating ownership validation to the application. */
  public static UpdateOlderAdultProfileCommand toCommand(
      String olderAdultId, UpdateOlderAdultProfileResource resource, AuthenticatedSession session) {
    if (session == null || session.role() != Role.CAREGIVER) {
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.CONSENT_REQUIRED,
          "only the registering caregiver can update profile data");
    }
    return new UpdateOlderAdultProfileCommand(
        session.subjectId(), olderAdultId, resource.fullName(), resource.birthDate());
  }
}
