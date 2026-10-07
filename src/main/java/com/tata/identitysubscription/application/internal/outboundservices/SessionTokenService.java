package com.tata.identitysubscription.application.internal.outboundservices;

import com.tata.identitysubscription.application.models.SessionResult;

public interface SessionTokenService {
    SessionResult issue(String accountId);
    default SessionResult issueOlderAdult(String olderAdultId) { return issue(olderAdultId); }
    default SessionResult issueLinkSetup(String olderAdultId, String careLinkId) { throw new UnsupportedOperationException(); }
    default java.util.Optional<com.tata.identitysubscription.application.models.AuthenticatedSession> authenticate(String token) { return java.util.Optional.empty(); }
    default void revoke(String token) { throw new UnsupportedOperationException(); }
}
