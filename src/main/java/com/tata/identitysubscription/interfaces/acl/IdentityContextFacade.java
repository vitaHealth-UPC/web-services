package com.tata.identitysubscription.interfaces.acl;

public interface IdentityContextFacade {
    boolean isEnabled(String accountId);
    com.tata.identitysubscription.application.models.SessionResult issueLinkSetup(String olderAdultId, String careLinkId);
    com.tata.identitysubscription.application.models.SessionResult issueOlderAdult(String olderAdultId);
}
