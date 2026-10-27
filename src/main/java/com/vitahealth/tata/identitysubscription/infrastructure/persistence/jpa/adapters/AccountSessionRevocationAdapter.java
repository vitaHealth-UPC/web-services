package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.RevokeAccountSessionsPort;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountSessionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

@Component
public class AccountSessionRevocationAdapter implements RevokeAccountSessionsPort {
  private final AccountSessionJpaRepository repository;

  public AccountSessionRevocationAdapter(AccountSessionJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void revokeCaregiverSessions(String accountId) {
    repository.revokeCaregiverSessions(accountId);
  }
}
