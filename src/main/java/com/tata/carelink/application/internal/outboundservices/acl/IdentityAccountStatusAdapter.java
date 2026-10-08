package com.tata.carelink.application.internal.outboundservices.acl;

import com.tata.carelink.application.internal.outboundservices.AccountStatusPort;
import com.tata.identitysubscription.interfaces.acl.IdentityContextFacade;
import org.springframework.stereotype.Component;

@Component
public class IdentityAccountStatusAdapter implements AccountStatusPort {
    private final IdentityContextFacade accountStatusQueryService;

    public IdentityAccountStatusAdapter(IdentityContextFacade accountStatusQueryService) {
        this.accountStatusQueryService = accountStatusQueryService;
    }

    @Override
    public boolean isEnabled(String accountId) {
        return accountStatusQueryService.isEnabled(accountId);
    }
}
