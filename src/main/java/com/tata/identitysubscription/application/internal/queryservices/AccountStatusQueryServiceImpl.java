package com.tata.identitysubscription.application.internal.queryservices;

import com.tata.identitysubscription.application.queryservices.AccountStatusQueryService;
import com.tata.identitysubscription.domain.repositories.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountStatusQueryServiceImpl implements AccountStatusQueryService {
    private final AccountRepository accountRepository;

    public AccountStatusQueryServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public boolean isEnabled(String accountId) {
        return accountRepository.findById(accountId)
                .map(account -> account.canAuthenticate())
                .orElse(false);
    }
}
