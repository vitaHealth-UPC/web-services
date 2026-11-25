package com.vitahealth.tata.identitysubscription.interfaces.acl;

import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/** Cross-context account status and relationship-scoped session issuance. */
public interface IdentityContextFacade {
  /** Checks whether the account can currently act as a caregiver. */
  boolean isEnabled(String accountId);

  /** Issues access restricted to the supplied consent onboarding relationship. */
  SessionResult issueLinkSetup(String olderAdultId, String careLinkId);

  /** Resolves a confirmed relationship for adult authentication through PIN. */
  SessionResult issueOlderAdult(String olderAdultId);

  /** Display name of an account, when it exists. */
  Optional<String> accountName(String accountId);

  /** Deletes the adult's PIN credential (account removal). */
  void removePinCredential(String olderAdultId);

  /** True when the adult already enrolled a PIN. */
  boolean hasPinCredential(String olderAdultId);

  /** True while some device holds an unexpired adult session. */
  boolean hasActiveOlderAdultSession(String olderAdultId);

  /** The adults among [olderAdultIds] holding an unexpired session, in one read. */
  Set<String> olderAdultsWithActiveSession(Collection<String> olderAdultIds);

  /** Issues adult access for the exact relationship confirmed by Care Link. */
  SessionResult issueOlderAdult(String olderAdultId, String careLinkId);
}
