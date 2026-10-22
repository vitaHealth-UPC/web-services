package com.vitahealth.tata.intakeexecution.infrastructure.acl;

import com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.vitahealth.tata.identitysubscription.application.exceptions.ResourceAccessDenied;
import com.vitahealth.tata.identitysubscription.application.queryservices.SessionRegistrationQueryService;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.IntakeConfirmationAccessPort;
import org.springframework.stereotype.Component;

/** Rechecks persisted sessions and live consent through public context boundaries. */
@Component
public class IdentityIntakeConfirmationAccessAdapter implements IntakeConfirmationAccessPort {
  private final SessionRegistrationQueryService sessions;
  private final CareLinkContextFacade links;

  /** Injects live session and relationship queries without accessing their persistence. */
  public IdentityIntakeConfirmationAccessAdapter(
      SessionRegistrationQueryService sessions, CareLinkContextFacade links) {
    this.sessions = sessions;
    this.links = links;
  }

  /** Rejects invalid credentials before the provider receives audio. */
  @Override
  public String authenticate(String credential) {
    return sessions
        .authenticateRegistration(credential)
        .orElseThrow(ResourceAccessDenied::new)
        .sessionId();
  }

  /** Rejects removed/expired sessions, setup credentials and lost caregiver consent. */
  @Override
  public void requireAuthorized(String reference, String olderAdultId) {
    var identity =
        sessions
            .findActiveRegistration(reference)
            .orElseThrow(ResourceAccessDenied::new)
            .identity();
    boolean allowed =
        switch (identity.role()) {
          case CAREGIVER -> links.isAuthorized(identity.subjectId(), olderAdultId);
          case OLDER_ADULT -> identity.subjectId().equals(olderAdultId);
          case LINK_SETUP -> false;
        };
    if (!allowed) throw new ResourceAccessDenied();
  }
}
