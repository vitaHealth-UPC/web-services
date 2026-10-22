package com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.assemblers.MedicationPersistenceAssembler;
import com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.repositories.MedicationJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Adapts medication aggregates to context-owned JPA persistence. */
@Repository
public class MedicationRepositoryImpl implements MedicationRepository {
  private final MedicationJpaRepository repository;

  private final EntityManager entities;

  /** Injects JPA storage and the caller-scoped persistence context. */
  public MedicationRepositoryImpl(MedicationJpaRepository repository, EntityManager entities) {
    this.entities = entities;
    this.repository = repository;
  }

  @Override
  public Medication save(Medication medication) {
    return MedicationPersistenceAssembler.toDomain(
        repository.save(MedicationPersistenceAssembler.toEntity(medication)));
  }

  @Override
  public Optional<Medication> findById(String id) {
    return repository.findById(id).map(MedicationPersistenceAssembler::toDomain);
  }

  @Override
  public List<Medication> findByOlderAdultId(String olderAdultId) {
    return repository.findByOlderAdultIdOrderByNameAsc(olderAdultId).stream()
        .map(MedicationPersistenceAssembler::toDomain)
        .toList();
  }

  /** Bounds the database result before returning ordered medication references. */
  @Override
  @Transactional(readOnly = true)
  public List<String> findIdsByOlderAdultIdAfter(String olderAdultId, String afterId, int limit) {
    if (limit < 1 || limit > 100)
      throw new IllegalArgumentException("medication page limit must be between 1 and 100");
    return List.copyOf(repository.findIdsAfter(olderAdultId, afterId, PageRequest.of(0, limit)));
  }

  /** Requires a transaction and refreshes any previously loaded snapshot under its row lock. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<Medication> findForUpdate(String id) {
    return repository
        .findForUpdate(id)
        .map(
            entity -> {
              entities.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
              return MedicationPersistenceAssembler.toDomain(entity);
            });
  }
}
