package com.tata.identitysubscription.application.internal.commandservices;

import com.tata.identitysubscription.application.internal.IdentityApplicationException;
import com.tata.identitysubscription.application.internal.SubscriptionMapper;
import com.tata.identitysubscription.application.models.SubscriptionResult;
import com.tata.identitysubscription.domain.model.commands.ChangeSubscriptionCommand;
import com.tata.identitysubscription.domain.repositories.AccountRepository;
import com.tata.identitysubscription.domain.services.PlanCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
public class ChangeSubscriptionCommandHandler {
    private final AccountRepository accountRepository;
    private final Clock clock;

    @Autowired
    public ChangeSubscriptionCommandHandler(AccountRepository accountRepository) {
        this(accountRepository, Clock.systemUTC());
    }

    ChangeSubscriptionCommandHandler(AccountRepository accountRepository, Clock clock) {
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    public SubscriptionResult handle(ChangeSubscriptionCommand command) {
        if (command.accountId() == null || command.accountId().isBlank()) {
            throw new IllegalArgumentException("accountId is required");
        }

        var account = accountRepository.findById(command.accountId().trim())
                .orElseThrow(() -> new IdentityApplicationException(
                        IdentityApplicationException.Code.ACCOUNT_NOT_FOUND,
                        "account not found"
                ));
        var plan = PlanCatalog.findByCode(command.planCode())
                .orElseThrow(() -> new IdentityApplicationException(
                        IdentityApplicationException.Code.PLAN_NOT_FOUND,
                        "subscription plan not found"
                ));

        try {
            if (account.changeSubscription(plan, clock.instant())) {
                account = accountRepository.save(account);
            }
        } catch (IllegalStateException exception) {
            throw new IdentityApplicationException(
                    IdentityApplicationException.Code.ACCOUNT_NOT_ACTIVE,
                    exception.getMessage()
            );
        }

        return SubscriptionMapper.toResult(account, plan);
    }
}
