package com.vitahealth.tata.carelink.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.assemblers.CareLinkPersistenceAssembler;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories.CareLinkJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps relationship persistence while preserving managed identity and command reservation scope.
 */
@Repository
public class CareLinkRepositoryImpl implements CareLinkRepository {
  private final CareLinkJpaRepository repository;
  private final Clock clock;

  /** Injects the context-owned persistence gateway. */
  public CareLinkRepositoryImpl(CareLinkJpaRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /** Inserts new identities or applies mutable domain state to the existing reserved row. */
  @Override
  @Transactional
  public CareLink save(CareLink careLink) {
    var state = CareLinkPersistenceAssembler.toEntity(careLink);
    state.releaseReservationIfExpired(clock.instant());
    var existing = repository.findByIdForUpdate(careLink.id());
    if (existing.isEmpty())
      return CareLinkPersistenceAssembler.toDomain(repository.saveAndFlush(state));
    var managed = existing.orElseThrow();
    managed.updateFrom(state);
    repository.flush();
    return CareLinkPersistenceAssembler.toDomain(managed);
  }

  /** Reads confirmed state and filters any evidence that does not grant current access. */
  @Override
  public List<CareLink> findConfirmedByCaregiver(String caregiverId) {
    return repository
        .findByCaregiverIdAndStatusOrderByConfirmedAtDescIdAsc(
            caregiverId, CareLinkStatus.CONFIRMED)
        .stream()
        .map(CareLinkPersistenceAssembler::toDomain)
        .filter(CareLink::isActive)
        .toList();
  }

  /** Reads a relationship snapshot without acquiring a write reservation. */
  @Override
  public Optional<CareLink> findById(String id) {
    return repository.findById(id).map(CareLinkPersistenceAssembler::toDomain);
  }

  /** Requires an existing command transaction so the row lock covers the complete transition. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<CareLink> findByIdForUpdate(String id) {
    return repository.findByIdForUpdate(id).map(CareLinkPersistenceAssembler::toDomain);
  }

  /** Requires an existing acceptance transaction so code consumption remains serialized. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<CareLink> findByCodeForUpdate(String code) {
    return repository
        .findByCodeForUpdate(code)
        .or(
            () ->
                repository.findHistoricalCodeForUpdate(code, PageRequest.of(0, 1)).stream()
                    .findFirst())
        .map(CareLinkPersistenceAssembler::toDomain);
  }

  /** Reads a matching code without consuming or reserving it. */
  @Override
  public Optional<CareLink> findByCode(String code) {
    return repository
        .findByReservedLinkingCode(code)
        .or(() -> repository.findFirstByLinkingCodeOrderByCodeExpiresAtDescIdAsc(code))
        .map(CareLinkPersistenceAssembler::toDomain);
  }

  /** Checks the unique active reservation without loading historical relationships. */
  @Override
  public boolean isCodeReserved(String code) {
    return repository.existsByReservedLinkingCode(code);
  }

  /** Keeps member-pair inspection in the issuance transaction and locks matching lifecycle rows. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public List<CareLink> findOpenMemberPairForUpdate(
      String caregiverId, String olderAdultId, Instant now) {
    return repository
        .findOpenMemberPairForUpdate(
            caregiverId,
            olderAdultId,
            CareLinkStatus.PENDING,
            List.of(CareLinkStatus.AWAITING_CONSENT, CareLinkStatus.CONFIRMED),
            now)
        .stream()
        .map(CareLinkPersistenceAssembler::toDomain)
        .toList();
  }

  /** Reads the confirmed member-pair relationship without changing its state. */
  @Override
  public Optional<CareLink> findConfirmed(String caregiverId, String olderAdultId) {
    return repository
        .findFirstByCaregiverIdAndOlderAdultIdAndStatus(
            caregiverId, olderAdultId, CareLinkStatus.CONFIRMED)
        .map(CareLinkPersistenceAssembler::toDomain);
  }

  /**
   * Keeps recipient scans bounded without loading unrelated relationship histories or credentials.
   */
  @Override
  @Transactional(readOnly = true)
  public List<String> findAuthorizedCaregiverIdsAfter(
      String olderAdultId, String afterId, int limit) {
    if (olderAdultId == null
        || olderAdultId.isBlank()
        || afterId == null
        || limit < 1
        || limit > 100) throw new IllegalArgumentException("caregiver recipient page is invalid");
    return repository.findAuthorizedCaregiverIdsAfter(
        olderAdultId, CareLinkStatus.CONFIRMED, afterId, PageRequest.of(0, limit));
  }
}
