package com.vitahealth.tata.identitysubscription.application.internal.queryservices;

import com.vitahealth.tata.identitysubscription.application.queryservices.AccountStatusQueryService;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads current account eligibility without creating accounts or changing their state. */
@Service
@Transactional(readOnly = true)
public class AccountStatusQueryServiceImpl implements AccountStatusQueryService {
  private final AccountRepository accountRepository;

  /** Injects Identity-owned account evidence. */
  public AccountStatusQueryServiceImpl(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  /** Returns false for an absent account while propagating technical read failures. */
  @Override
  public boolean isEnabled(String accountId) {
    return accountRepository
        .findById(accountId)
        .map(account -> account.canAuthenticate())
        .orElse(false);
  }
}
