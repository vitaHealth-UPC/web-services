package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.OlderAdultSessionLookup;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountSessionJpaRepository;
import java.time.Clock;
import java.util.Collection;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Answers from stored session evidence; signing out deletes it, so it reflects the device. */
@Component
public class OlderAdultSessionLookupAdapter implements OlderAdultSessionLookup {
  private final AccountSessionJpaRepository sessions;
  private final Clock clock;

  /** Injects session storage and the application clock. */
  public OlderAdultSessionLookupAdapter(AccountSessionJpaRepository sessions, Clock clock) {
    this.sessions = sessions;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasActiveSession(String olderAdultId) {
    return sessions.existsByAccountIdAndRoleAndExpiresAtAfter(
        olderAdultId, "OLDER_ADULT", clock.instant());
  }

  @Override
  @Transactional(readOnly = true)
  public Set<String> withActiveSession(Collection<String> olderAdultIds) {
    return Set.copyOf(
        sessions.findLiveAccountIds(olderAdultIds, "OLDER_ADULT", clock.instant()));
  }
}
