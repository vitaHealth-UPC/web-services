package com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import java.util.Optional;

/** Issues role-specific credentials and validates expiry, revocation and effective identity. */
public interface SessionTokenService {
  /** Issues a caregiver credential for an active account. */
  SessionResult issue(String accountId);

  /** Issues an adult credential without treating that identity as a caregiver account. */
  SessionResult issueOlderAdult(String olderAdultId);

  /** Issues adult access for the exact confirmed relationship supplied by Care Link. */
  SessionResult issueOlderAdult(String olderAdultId, String careLinkId);

  /** Issues a credential restricted to the referenced consent onboarding workflow. */
  SessionResult issueLinkSetup(String olderAdultId, String careLinkId);

  /** Returns effective identity only for a currently valid credential. */
  Optional<AuthenticatedSession> authenticate(String token);

  /** Revokes the credential so later authentication cannot reuse it. */
  void revoke(String token);
}
