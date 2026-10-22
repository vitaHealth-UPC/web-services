package com.vitahealth.tata.carelink.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.assemblers.OlderAdultProfilePersistenceAssembler;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories.OlderAdultProfileJpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Maps adult profiles while preserving managed ownership and domain creation evidence. */
@Repository
public class OlderAdultProfileRepositoryImpl implements OlderAdultProfileRepository {
  private final OlderAdultProfileJpaRepository repository;

  /** Injects the context-owned profile persistence gateway. */
  public OlderAdultProfileRepositoryImpl(OlderAdultProfileJpaRepository repository) {
    this.repository = repository;
  }

  /** Loads profile snapshots in one batch without depending on persistence return order. */
  @Override
  public List<OlderAdultProfile> findAllByIds(Collection<String> ids) {
    if (ids.isEmpty()) return List.of();
    return repository.findAllById(ids).stream()
        .map(OlderAdultProfilePersistenceAssembler::toDomain)
        .toList();
  }

  /** Inserts new identities or updates the mutable state of the reserved existing entity. */
  @Override
  @Transactional
  public OlderAdultProfile save(OlderAdultProfile profile) {
    var state = OlderAdultProfilePersistenceAssembler.toEntity(profile);
    var existing = repository.findByIdForUpdate(profile.id());
    if (existing.isEmpty())
      return OlderAdultProfilePersistenceAssembler.toDomain(repository.saveAndFlush(state));
    var managed = existing.orElseThrow();
    managed.updateFrom(state);
    repository.flush();
    return OlderAdultProfilePersistenceAssembler.toDomain(managed);
  }

  /** Reads current profile evidence without acquiring a mutation reservation. */
  @Override
  public Optional<OlderAdultProfile> findById(String id) {
    return repository.findById(id).map(OlderAdultProfilePersistenceAssembler::toDomain);
  }

  /** Requires the caller's transaction so profile update reservations remain effective. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<OlderAdultProfile> findByIdForUpdate(String id) {
    return repository.findByIdForUpdate(id).map(OlderAdultProfilePersistenceAssembler::toDomain);
  }
}
