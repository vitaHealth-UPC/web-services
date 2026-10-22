package com.vitahealth.tata.carelink.infrastructure.events;

import com.vitahealth.tata.carelink.domain.model.events.CareLinkConsentWithdrawn;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.entities.ConsentWithdrawalEvidenceEntity;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories.ConsentWithdrawalEvidenceJpaRepository;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Persists original approval and withdrawal evidence in the relationship command transaction. */
@Component
public class ConsentWithdrawalEvidenceEventHandler {
  private final ConsentWithdrawalEvidenceJpaRepository evidence;

  /** Injects this context's decision-evidence gateway. */
  public ConsentWithdrawalEvidenceEventHandler(ConsentWithdrawalEvidenceJpaRepository evidence) {
    this.evidence = evidence;
  }

  /** Ensures failure to retain decision history rolls back withdrawal and session invalidation. */
  @EventListener
  @Order(0)
  @Transactional(propagation = Propagation.MANDATORY)
  public void handle(CareLinkConsentWithdrawn event) {
    evidence.saveAndFlush(
        new ConsentWithdrawalEvidenceEntity(
            event.careLinkId(),
            event.caregiverId(),
            event.olderAdultId(),
            event.approvedAt(),
            event.withdrawnAt()));
  }
}
