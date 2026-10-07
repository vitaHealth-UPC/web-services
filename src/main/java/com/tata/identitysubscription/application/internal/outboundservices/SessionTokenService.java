package com.tata.identitysubscription.application.internal.outboundservices;

import com.tata.identitysubscription.application.models.AuthenticatedSubject;
import com.tata.identitysubscription.application.models.SessionResult;
import java.util.Optional;

public interface SessionTokenService {
    SessionResult issue(String accountId);

    /**
     * Resolves a raw Bearer token to its owning subject when the session is still valid.
     */
    Optional<AuthenticatedSubject> authenticate(String rawAccessToken);
}
