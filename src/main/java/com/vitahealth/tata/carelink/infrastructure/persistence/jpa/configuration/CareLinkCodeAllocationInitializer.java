package com.vitahealth.tata.carelink.infrastructure.persistence.jpa.configuration;

import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.entities.CareLinkCodeNamespaceEntity;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories.CareLinkCodeNamespaceJpaRepository;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories.CareLinkJpaRepository;
import com.vitahealth.tata.shared.infrastructure.persistence.jpa.exceptions.UniqueConstraintClassifier;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Initializes issuance coordination and reconciles live reservations before issuers become ready.
 */
@Component
public class CareLinkCodeAllocationInitializer {
  /** Logical namespace coordinated by every short-code issuance transaction. */
  public static final String NAMESPACE = "linking-codes";

  private final CareLinkCodeNamespaceJpaRepository namespaces;
  private final CareLinkJpaRepository links;
  private final PlatformTransactionManager transactions;
  private final Clock clock;

  /**
   * Injects namespace persistence, reservation reconciliation and explicit initialization
   * boundaries.
   */
  public CareLinkCodeAllocationInitializer(
      CareLinkCodeNamespaceJpaRepository namespaces,
      CareLinkJpaRepository links,
      PlatformTransactionManager transactions,
      Clock clock) {
    this.namespaces = namespaces;
    this.links = links;
    this.transactions = transactions;
    this.clock = clock;
  }

  /**
   * Creates a namespace once and backfills live reservations under its lock. Only the named
   * namespace insertion race is recovered after rollback; conflicting live codes or pairs fail
   * startup without deleting historical rows.
   */
  @PostConstruct
  public void initialize() {
    var transaction = new TransactionTemplate(transactions);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    try {
      transaction.executeWithoutResult(
          status -> {
            if (namespaces.findByNamespace(NAMESPACE).isEmpty())
              namespaces.saveAndFlush(new CareLinkCodeNamespaceEntity(NAMESPACE));
          });
    } catch (DataIntegrityViolationException exception) {
      if (!UniqueConstraintClassifier.matches(exception, "uk_care_link_code_namespace")
          || namespaces.findByNamespace(NAMESPACE).isEmpty()) throw exception;
    }
    transaction.executeWithoutResult(
        status -> {
          namespaces.findByNamespaceForUpdate(NAMESPACE).orElseThrow();
          var now = clock.instant();
          links.releaseExpiredCodeReservations(CareLinkStatus.PENDING, now);
          links.reserveUnclaimedLiveCodes(CareLinkStatus.PENDING, now);
          links.reserveUnclaimedMemberPairs(
              CareLinkStatus.PENDING,
              List.of(CareLinkStatus.AWAITING_CONSENT, CareLinkStatus.CONFIRMED),
              now);
        });
  }
}
