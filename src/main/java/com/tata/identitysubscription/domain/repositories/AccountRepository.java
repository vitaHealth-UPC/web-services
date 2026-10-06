package com.tata.identitysubscription.domain.repositories;

import com.tata.identitysubscription.domain.model.aggregates.Account;
import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findByEmail(String email);
    Account save(Account account);
}
