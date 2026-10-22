package com.vitahealth.tata.carelink.application.internal.commandservices;

import com.vitahealth.tata.carelink.application.internal.outboundservices.LinkSessionPort;
import com.vitahealth.tata.carelink.application.result.AuthenticatedCareLinkResult;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps relationship transitions and credential issuance in one rollback boundary. */
@Service
@Transactional
public class LinkAuthenticationCommandHandler {
  private final AcceptCareLinkCommandHandler acceptances;
  private final RegisterConsentCommandHandler consents;
  private final LinkSessionPort sessions;

  /** Injects transactional relationship handlers and the credential issuance boundary. */
  public LinkAuthenticationCommandHandler(
      AcceptCareLinkCommandHandler acceptances,
      RegisterConsentCommandHandler consents,
      LinkSessionPort sessions) {
    this.acceptances = acceptances;
    this.consents = consents;
    this.sessions = sessions;
  }

  /** Accepts the linking code and issues the restricted onboarding credential atomically. */
  public AuthenticatedCareLinkResult accept(AcceptCareLinkCommand command) {
    return sessions.establish(acceptances.handle(command));
  }

  /** Issues adult access only for first approval; rejection and replay return no new credential. */
  public AuthenticatedCareLinkResult registerConsent(RegisterConsentCommand command) {
    var decision = consents.handleDecision(command);
    return command.accepted() && decision.changed()
        ? sessions.establish(decision.link())
        : new AuthenticatedCareLinkResult(decision.link(), null, null);
  }
}
