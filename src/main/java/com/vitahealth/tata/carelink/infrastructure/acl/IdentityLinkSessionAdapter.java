package com.vitahealth.tata.carelink.infrastructure.acl;

import com.vitahealth.tata.carelink.application.internal.outboundservices.LinkSessionPort;
import com.vitahealth.tata.carelink.application.result.AuthenticatedCareLinkResult;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.interfaces.acl.IdentityContextFacade;
import org.springframework.stereotype.Component;

/** Exchanges the current consent workflow result for a credential scoped to that relationship. */
@Component
public class IdentityLinkSessionAdapter implements LinkSessionPort {
  private final IdentityContextFacade identity;

  /** Injects the Identity public boundary for credential issuance. */
  public IdentityLinkSessionAdapter(IdentityContextFacade identity) {
    this.identity = identity;
  }

  /** Issues onboarding or confirmed access without choosing another adult relationship. */
  @Override
  public AuthenticatedCareLinkResult establish(CareLinkResult link) {
    SessionResult session;
    if (link.status() == CareLinkStatus.CONFIRMED
        && link.consentGranted()
        && link.confirmedAt() != null) {
      session = identity.issueOlderAdult(link.olderAdultId(), link.id());
    } else if (link.status() == CareLinkStatus.AWAITING_CONSENT
        && link.codeUsedAt() != null
        && !link.consentGranted()
        && link.confirmedAt() == null) {
      session = identity.issueLinkSetup(link.olderAdultId(), link.id());
    } else {
      throw new IllegalArgumentException(
          "care relationship cannot establish a session in this state");
    }
    return new AuthenticatedCareLinkResult(link, session.accessToken(), session.expiresAt());
  }
}
