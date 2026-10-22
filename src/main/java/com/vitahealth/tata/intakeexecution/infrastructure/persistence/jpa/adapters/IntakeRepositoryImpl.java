package com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeReportingCandidate;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.assemblers.IntakePersistenceAssembler;
import com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.repositories.IntakeJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Maps persistence and holds transition locks in the caller transaction. */
@Repository
public class IntakeRepositoryImpl implements IntakeRepository {
  private final IntakeJpaRepository repository;

  private final EntityManager entities;

  /** Injects storage and the caller-scoped persistence context for locked refreshes. */
  public IntakeRepositoryImpl(IntakeJpaRepository repository, EntityManager entities) {
    this.entities = entities;
    this.repository = repository;
  }

  @Override
  public List<Intake> findAgenda(String olderAdultId, Instant from, Instant to) {
    return repository.findAgenda(olderAdultId, from, to).stream()
        .map(IntakePersistenceAssembler::toDomain)
        .toList();
  }

  @Override
  public List<String> findOlderAdultIdsWithIntakes(
      Instant from, Instant to, String after, int limit) {
    return repository.findOlderAdultIdsWithIntakes(from, to, after, PageRequest.of(0, limit));
  }

  @Override
  public List<Intake> saveAll(List<Intake> intakes) {
    return repository
        .saveAll(intakes.stream().map(IntakePersistenceAssembler::toEntity).toList())
        .stream()
        .map(IntakePersistenceAssembler::toDomain)
        .toList();
  }

  @Override
  public Optional<Intake> findById(String id) {
    return repository.findById(id).map(IntakePersistenceAssembler::toDomain);
  }

  /** Refreshes managed evidence after taking the lock, including previously loaded rows. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<Intake> findByIdForConfirmation(String id) {
    return repository
        .findByIdForConfirmation(id)
        .map(
            entity -> {
              entities.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
              return IntakePersistenceAssembler.toDomain(entity);
            });
  }

  /**
   * Reads a bounded reference page in its own short read transaction, without reserving sources.
   */
  @Override
  @Transactional(readOnly = true)
  public List<IntakeReportingCandidate> findUnreportedPendingDueAfter(
      Instant cutoff, IntakeReportingCandidate after, int limit) {
    if (cutoff == null || limit < 1 || limit > 200)
      throw new IllegalArgumentException(
          "candidate scan requires a cutoff and limit from one to 200");
    var page = PageRequest.of(0, limit);
    var candidates =
        after == null
            ? repository.findReportingCandidates(IntakeStatus.PENDING, cutoff, page)
            : repository.findReportingCandidatesAfter(
                IntakeStatus.PENDING, cutoff, after.scheduledAt(), after.id(), page);
    return candidates.stream()
        .map(row -> new IntakeReportingCandidate(row.getId(), row.getScheduledAt()))
        .toList();
  }

  @Override
  public List<Intake> findFutureByTreatmentId(String treatmentId, Instant from) {
    return repository
        .findByTreatmentIdAndScheduledAtGreaterThanEqualOrderByScheduledAtAscIdAsc(
            treatmentId, from)
        .stream()
        .map(IntakePersistenceAssembler::toDomain)
        .toList();
  }

  @Override
  public void deleteAll(List<Intake> intakes) {
    repository.deleteAll(intakes.stream().map(IntakePersistenceAssembler::toEntity).toList());
  }

  @Override
  public Optional<Intake> findNextPendingByOlderAdultId(String olderAdultId, Instant from) {
    return repository
        .findFirstByOlderAdultIdAndStatusAndScheduledAtGreaterThanEqualOrderByScheduledAtAscIdAsc(
            olderAdultId, IntakeStatus.PENDING, from)
        .map(IntakePersistenceAssembler::toDomain);
  }

  /** Requires a caller transaction and refreshes slot evidence after acquiring its row lock. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public List<Intake> findFutureByTreatmentIdForUpdate(String treatmentId, Instant from) {
    return repository.findFutureByTreatmentIdForUpdate(treatmentId, from).stream()
        .map(
            entity -> {
              entities.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
              return IntakePersistenceAssembler.toDomain(entity);
            })
        .toList();
  }
}
