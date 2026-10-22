package com.vitahealth.tata.identitysubscription.application.internal.acl;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.application.queryservices.AccountStatusQueryService;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.interfaces.acl.IdentityContextFacade;
import org.springframework.stereotype.Service;

/** Delegates account reads and scoped credential issuance to Identity application ports. */
@Service
public class IdentityContextFacadeImpl implements IdentityContextFacade {
  private final AccountStatusQueryService queries;
  private final SessionTokenService tokens;

  /** Injects account status and credential issuance responsibilities. */
  public IdentityContextFacadeImpl(AccountStatusQueryService queries, SessionTokenService tokens) {
    this.queries = queries;
    this.tokens = tokens;
  }

  /** Issues access restricted to the supplied relationship onboarding flow. */
  @Override
  public SessionResult issueLinkSetup(String owner, String linkId) {
    return tokens.issueLinkSetup(owner, linkId);
  }

  /** Issues adult access through Identity's current relationship selection. */
  @Override
  public SessionResult issueOlderAdult(String owner) {
    return tokens.issueOlderAdult(owner);
  }

  /** Preserves the exact relationship selected by the consent workflow. */
  @Override
  public SessionResult issueOlderAdult(String owner, String linkId) {
    return tokens.issueOlderAdult(owner, linkId);
  }

  /** Reads live caregiver account eligibility without accessing persistence here. */
  @Override
  public boolean isEnabled(String accountId) {
    return queries.isEnabled(accountId);
  }
}
