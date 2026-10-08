package com.tata.identitysubscription.application.acl;


import com.tata.identitysubscription.interfaces.acl.IdentityContextFacade;
import com.tata.identitysubscription.application.queryservices.AccountStatusQueryService;
import org.springframework.stereotype.Service;

@Service
public class IdentityContextFacadeImpl implements IdentityContextFacade {
    private final AccountStatusQueryService queries;
    private final com.tata.identitysubscription.application.internal.outboundservices.SessionTokenService tokens;
    public IdentityContextFacadeImpl(AccountStatusQueryService queries, com.tata.identitysubscription.application.internal.outboundservices.SessionTokenService tokens) { this.queries = queries; this.tokens=tokens; }
    @Override public com.tata.identitysubscription.application.models.SessionResult issueLinkSetup(String owner,String linkId) { return tokens.issueLinkSetup(owner,linkId); }
    @Override public com.tata.identitysubscription.application.models.SessionResult issueOlderAdult(String owner) { return tokens.issueOlderAdult(owner); }
    @Override public boolean isEnabled(String accountId) { return queries.isEnabled(accountId); }
}
