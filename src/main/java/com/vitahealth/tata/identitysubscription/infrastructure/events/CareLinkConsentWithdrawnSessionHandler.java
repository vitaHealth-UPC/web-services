package com.vitahealth.tata.identitysubscription.infrastructure.events;

import com.vitahealth.tata.carelink.domain.model.events.CareLinkConsentWithdrawn;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountSessionJpaRepository;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Invalidates Identity's own relationship-scoped credentials when the adult withdraws consent. */
@Component
public class CareLinkConsentWithdrawnSessionHandler {
  private final AccountSessionJpaRepository sessions;

  /** Injects Identity session persistence without querying Care Link's private database tables. */
  public CareLinkConsentWithdrawnSessionHandler(AccountSessionJpaRepository sessions) {
    this.sessions = sessions;
  }

  /** Revokes scoped credentials in the same transaction as the committed consent decision. */
  @EventListener
  @Order(1)
  @Transactional(propagation = Propagation.MANDATORY)
  public void handle(CareLinkConsentWithdrawn event) {
    sessions.deleteByCareLinkId(event.careLinkId());
  }
}
