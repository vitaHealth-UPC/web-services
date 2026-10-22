package com.vitahealth.tata.identitysubscription.application.internal.queryservices;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.application.queryservices.SessionQueryService;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Resolves a bearer session without issuing or revoking credentials. */
@Service
public class SessionQueryServiceImpl implements SessionQueryService {
  private final SessionTokenService tokens;

  /** Injects the identity-owned credential lookup port without depending on token technology. */
  public SessionQueryServiceImpl(SessionTokenService tokens) {
    this.tokens = tokens;
  }

  /** Returns an authenticated subject only while its session and resource access remain valid. */
  @Override
  public Optional<AuthenticatedSession> authenticate(String token) {
    return tokens.authenticate(token);
  }
}
