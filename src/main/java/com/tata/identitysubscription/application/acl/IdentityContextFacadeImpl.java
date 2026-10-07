package com.tata.identitysubscription.application.acl;


import com.tata.identitysubscription.interfaces.acl.IdentityContextFacade;
import com.tata.identitysubscription.application.queryservices.AccountStatusQueryService;
import org.springframework.stereotype.Service;

@Service
public class IdentityContextFacadeImpl implements IdentityContextFacade {
    private final AccountStatusQueryService queries;
    public IdentityContextFacadeImpl(AccountStatusQueryService queries) { this.queries = queries; }
    @Override public boolean isEnabled(String accountId) { return queries.isEnabled(accountId); }
}
