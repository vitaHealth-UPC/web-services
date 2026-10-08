package com.tata.identitysubscription.application.internal.queryservices;

import com.tata.identitysubscription.application.IdentityApplicationException;
import com.tata.identitysubscription.application.internal.SubscriptionMapper;
import com.tata.identitysubscription.application.models.SubscriptionResult;
import com.tata.identitysubscription.domain.repositories.AccountRepository;
import com.tata.identitysubscription.domain.services.PlanCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetCurrentSubscriptionQueryHandler implements com.tata.identitysubscription.application.queryservices.GetCurrentSubscriptionQueryService {
    private final AccountRepository accountRepository;

    public GetCurrentSubscriptionQueryHandler(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public SubscriptionResult handle(String accountId) {
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IdentityApplicationException(
                        IdentityApplicationException.Code.ACCOUNT_NOT_FOUND,
                        "account not found"
                ));
        var plan = PlanCatalog.findByCode(account.currentPlanCode())
                .orElseThrow(() -> new IdentityApplicationException(
                        IdentityApplicationException.Code.PLAN_NOT_FOUND,
                        "subscription plan not found"
                ));
        return SubscriptionMapper.toResult(account, plan);
    }
}
