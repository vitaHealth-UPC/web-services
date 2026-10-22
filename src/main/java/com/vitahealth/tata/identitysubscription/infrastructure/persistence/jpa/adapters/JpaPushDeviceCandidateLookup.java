package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.PushDeviceCandidateLookup;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PushDeviceRegistration;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.assemblers.PushDeviceRegistrationPersistenceAssembler;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.PushDeviceRegistrationJpaRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Binds authorized-address candidate selection to bounded ordered SQL without dispatching messages.
 */
@Repository
public class JpaPushDeviceCandidateLookup implements PushDeviceCandidateLookup {
  private final PushDeviceRegistrationJpaRepository rows;

  /** Injects the provider-independent device/session storage query. */
  public JpaPushDeviceCandidateLookup(PushDeviceRegistrationJpaRepository rows) {
    this.rows = rows;
  }

  /** Preserves the exact device token while restricting candidate owner, role and expiry in SQL. */
  @Override
  @Transactional(readOnly = true)
  public List<PushDeviceRegistration> findCandidates(
      String subjectId, Role role, String afterId, int limit, Instant now) {
    if (subjectId == null
        || subjectId.isBlank()
        || role == null
        || role == Role.LINK_SETUP
        || afterId == null
        || limit < 1
        || limit > 100
        || now == null) throw new IllegalArgumentException("device candidate scope is invalid");
    return rows
        .findCandidates(subjectId, role.name(), afterId, now, PageRequest.of(0, limit))
        .stream()
        .map(PushDeviceRegistrationPersistenceAssembler::toDomain)
        .toList();
  }
}
