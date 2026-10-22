package com.vitahealth.tata.identitysubscription.application.internal.queryservices;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionRegistrationLookup;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.application.queryservices.SessionRegistrationQueryService;
import com.vitahealth.tata.identitysubscription.application.result.RegisteredSessionResult;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Keeps signature verification and persisted session ownership on the same registration boundary.
 */
@Service
public class SessionRegistrationQueryServiceImpl implements SessionRegistrationQueryService {
  private final SessionTokenService tokens;
  private final SessionRegistrationLookup registrations;

  /**
   * Injects token verification and live registration evidence independently of persistence format.
   */
  public SessionRegistrationQueryServiceImpl(
      SessionTokenService tokens, SessionRegistrationLookup registrations) {
    this.tokens = tokens;
    this.registrations = registrations;
  }

  /** Rejects revocation or identity changes between signature verification and reference lookup. */
  @Override
  public Optional<RegisteredSessionResult> authenticateRegistration(String credential) {
    return tokens
        .authenticate(credential)
        .flatMap(
            identity ->
                registrations
                    .findRegistration(credential)
                    .filter(registration -> registration.identity().equals(identity)));
  }

  /** Delegates fresh live authorization; no cached principal grants future notification access. */
  @Override
  public Optional<RegisteredSessionResult> findActiveRegistration(String sessionId) {
    return registrations.findActiveRegistration(sessionId);
  }
}
