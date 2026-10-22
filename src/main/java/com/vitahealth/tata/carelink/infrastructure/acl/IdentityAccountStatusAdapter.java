package com.vitahealth.tata.carelink.infrastructure.acl;

import com.vitahealth.tata.carelink.application.internal.outboundservices.AccountStatusPort;
import com.vitahealth.tata.identitysubscription.interfaces.acl.IdentityContextFacade;
import org.springframework.stereotype.Component;

/** Delegates caregiver eligibility to Identity without reading its private persistence. */
@Component
public class IdentityAccountStatusAdapter implements AccountStatusPort {
  private final IdentityContextFacade identity;

  /** Injects the public account-status facade owned by Identity. */
  public IdentityAccountStatusAdapter(IdentityContextFacade identity) {
    this.identity = identity;
  }

  /** Preserves the absence-versus-technical-failure contract of the Identity lookup. */
  @Override
  public boolean isEnabled(String accountId) {
    return identity.isEnabled(accountId);
  }
}
