package com.vitahealth.tata.carelink.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.carelink.application.internal.outboundservices.LinkingCodeAllocationPort;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.configuration.CareLinkCodeAllocationInitializer;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories.CareLinkCodeNamespaceJpaRepository;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories.CareLinkJpaRepository;
import java.time.Instant;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Reserves short-code issuance without introducing a transaction separate from the command. */
@Component
@DependsOn("careLinkCodeAllocationInitializer")
@Transactional(propagation = Propagation.MANDATORY)
public class JpaLinkingCodeAllocationAdapter implements LinkingCodeAllocationPort {
  private final CareLinkCodeNamespaceJpaRepository namespaces;
  private final CareLinkJpaRepository links;

  /** Injects context-owned coordination and reservation persistence. */
  public JpaLinkingCodeAllocationAdapter(
      CareLinkCodeNamespaceJpaRepository namespaces, CareLinkJpaRepository links) {
    this.namespaces = namespaces;
    this.links = links;
  }

  /** Holds the database namespace lock until the caller commits or rolls back. */
  @Override
  public void reserveNamespace() {
    namespaces.findByNamespaceForUpdate(CareLinkCodeAllocationInitializer.NAMESPACE).orElseThrow();
  }

  /** Frees expired technical reservations while preserving historical domain timestamps. */
  @Override
  public void releaseExpiredReservations(Instant now) {
    links.releaseExpiredCodeReservations(CareLinkStatus.PENDING, now);
  }
}
