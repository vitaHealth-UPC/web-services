package com.vitahealth.tata.identitysubscription.interfaces.acl;

import com.vitahealth.tata.identitysubscription.application.result.SessionResult;

/** Cross-context account status and relationship-scoped session issuance. */
public interface IdentityContextFacade {
  /** Checks whether the account can currently act as a caregiver. */
  boolean isEnabled(String accountId);

  /** Issues access restricted to the supplied consent onboarding relationship. */
  SessionResult issueLinkSetup(String olderAdultId, String careLinkId);

  /** Resolves a confirmed relationship for adult authentication through PIN. */
  SessionResult issueOlderAdult(String olderAdultId);

  /** Issues adult access for the exact relationship confirmed by Care Link. */
  SessionResult issueOlderAdult(String olderAdultId, String careLinkId);
}
