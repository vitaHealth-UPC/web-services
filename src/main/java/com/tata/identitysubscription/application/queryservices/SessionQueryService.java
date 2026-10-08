package com.tata.identitysubscription.application.queryservices;
import com.tata.identitysubscription.application.models.AuthenticatedSession;
import java.util.Optional;
public interface SessionQueryService { Optional<AuthenticatedSession> authenticate(String token); }
