package com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.assemblers.TreatmentPersistenceAssembler;
import com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.repositories.TreatmentJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Adapts treatment aggregates to context-owned JPA persistence. */
@Repository
public class TreatmentRepositoryImpl implements TreatmentRepository {
  private final TreatmentJpaRepository repository;

  private final EntityManager entities;

  /** Injects JPA storage and the caller-scoped persistence context. */
  public TreatmentRepositoryImpl(TreatmentJpaRepository repository, EntityManager entities) {
    this.entities = entities;
    this.repository = repository;
  }

  @Override
  public Treatment save(Treatment treatment) {
    return TreatmentPersistenceAssembler.toDomain(
        repository.save(TreatmentPersistenceAssembler.toEntity(treatment)));
  }

  @Override
  public Optional<Treatment> findById(String id) {
    return repository.findById(id).map(TreatmentPersistenceAssembler::toDomain);
  }

  @Override
  public List<Treatment> findByOlderAdultId(String olderAdultId) {
    return repository.findByOlderAdultIdOrderByCreatedAtAsc(olderAdultId).stream()
        .map(TreatmentPersistenceAssembler::toDomain)
        .toList();
  }

  @Override
  public List<Treatment> findByMedicationId(String medicationId) {
    return repository.findByMedicationId(medicationId).stream()
        .map(TreatmentPersistenceAssembler::toDomain)
        .toList();
  }

  @Override
  public List<Treatment> findByStatus(TreatmentStatus status) {
    return repository.findByStatus(status).stream()
        .map(TreatmentPersistenceAssembler::toDomain)
        .toList();
  }

  /** Requires a transaction and refreshes any previously loaded snapshot under its row lock. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<Treatment> findForUpdate(String id) {
    return repository
        .findForUpdate(id)
        .map(
            entity -> {
              entities.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
              return TreatmentPersistenceAssembler.toDomain(entity);
            });
  }

  /** Bounds each candidate read and leaves transaction ownership to each subsequent extension. */
  @Override
  public List<String> findIdsByStatusAfter(TreatmentStatus status, String afterId, int limit) {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(afterId, "afterId");
    if (limit < 1 || limit > 500)
      throw new IllegalArgumentException("candidate page limit must be between 1 and 500");
    return List.copyOf(repository.findIdsByStatusAfter(status, afterId, PageRequest.of(0, limit)));
  }
}
