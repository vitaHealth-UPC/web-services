package com.vitahealth.tata.identitysubscription.application.internal.acl;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.OlderAdultSessionLookup;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.application.queryservices.AccountStatusQueryService;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.identitysubscription.domain.repositories.PinCredentialRepository;
import com.vitahealth.tata.identitysubscription.interfaces.acl.IdentityContextFacade;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Delegates account reads and scoped credential issuance to Identity application ports. */
@Service
public class IdentityContextFacadeImpl implements IdentityContextFacade {
  private final AccountStatusQueryService queries;
  private final SessionTokenService tokens;
  private final PinCredentialRepository pins;
  private final OlderAdultSessionLookup adultSessions;
  private final AccountRepository accounts;

  /** Injects account status and credential issuance responsibilities. */
  public IdentityContextFacadeImpl(
      AccountStatusQueryService queries,
      SessionTokenService tokens,
      PinCredentialRepository pins,
      OlderAdultSessionLookup adultSessions,
      AccountRepository accounts) {
    this.queries = queries;
    this.tokens = tokens;
    this.pins = pins;
    this.adultSessions = adultSessions;
    this.accounts = accounts;
  }

  /** Reads an account's display name for relationship requests. */
  @Override
  public Optional<String> accountName(String accountId) {
    return accounts.findById(accountId).map(account -> account.name());
  }

  /** Deletes the adult's PIN credential when the adult removes their account. */
  @Override
  @Transactional
  public void removePinCredential(String olderAdultId) {
    pins.deleteByOlderAdultId(olderAdultId);
  }

  /** Reports PIN enrollment without exposing the credential. */
  @Override
  public boolean hasPinCredential(String olderAdultId) {
    return pins.findByOlderAdultId(olderAdultId).isPresent();
  }

  /** Reads which adults hold a live session, in one batch. */
  @Override
  public Set<String> olderAdultsWithActiveSession(Collection<String> olderAdultIds) {
    return olderAdultIds.isEmpty() ? Set.of() : adultSessions.withActiveSession(olderAdultIds);
  }

  /** Reports whether a device currently holds an adult session. */
  @Override
  public boolean hasActiveOlderAdultSession(String olderAdultId) {
    return adultSessions.hasActiveSession(olderAdultId);
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
