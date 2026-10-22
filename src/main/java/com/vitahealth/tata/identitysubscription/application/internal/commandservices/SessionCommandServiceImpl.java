package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import com.vitahealth.tata.identitysubscription.application.commandservices.SessionCommandService;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import org.springframework.stereotype.Service;

/** Invalidates bearer credentials through the token port owned by identity. */
@Service
public class SessionCommandServiceImpl implements SessionCommandService {
  private final SessionTokenService tokens;

  /** Injects the port responsible for durable credential revocation. */
  public SessionCommandServiceImpl(SessionTokenService tokens) {
    this.tokens = tokens;
  }

  /** Revokes the supplied session; retrying a revocation is harmless. */
  @Override
  public void revoke(String token) {
    tokens.revoke(token);
  }
}
