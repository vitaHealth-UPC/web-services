package com.tata.identitysubscription.application.internal.queryservices;
import com.tata.identitysubscription.application.commandservices.SessionCommandService;
import com.tata.identitysubscription.application.queryservices.SessionQueryService;
import com.tata.identitysubscription.application.internal.outboundservices.SessionTokenService;
import com.tata.identitysubscription.application.models.AuthenticatedSession;
import java.util.Optional;
import org.springframework.stereotype.Service;
@Service
public class SessionServiceImpl implements SessionQueryService, SessionCommandService {
 private final SessionTokenService tokens;
 public SessionServiceImpl(SessionTokenService tokens) { this.tokens=tokens; }
 public Optional<AuthenticatedSession> authenticate(String token) { return tokens.authenticate(token); }
 public void revoke(String token) { tokens.revoke(token); }
}
