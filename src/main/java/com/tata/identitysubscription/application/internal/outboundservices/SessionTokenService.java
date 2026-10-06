package com.tata.identitysubscription.application.internal.outboundservices;

import com.tata.identitysubscription.application.models.SessionResult;

public interface SessionTokenService {
    SessionResult issue(String accountId);
}
