package com.tata.carelink.infrastructure.external;

import com.tata.carelink.application.internal.outboundservices.AccountStatusPort;
import com.tata.identitysubscription.application.queryservices.AccountStatusQueryService;
import org.springframework.stereotype.Component;

@Component
public class IdentityAccountStatusAdapter implements AccountStatusPort {
    private final AccountStatusQueryService accountStatusQueryService;

    public IdentityAccountStatusAdapter(AccountStatusQueryService accountStatusQueryService) {
        this.accountStatusQueryService = accountStatusQueryService;
    }

    @Override
    public boolean isEnabled(String accountId) {
        return accountStatusQueryService.isEnabled(accountId);
    }
}
