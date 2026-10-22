package com.vitahealth.tata.adherenceanalytics.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.adherenceanalytics.domain.exceptions.AdherenceSnapshotAlreadyExistsException;
import com.vitahealth.tata.adherenceanalytics.domain.model.aggregates.AdherencePeriodSnapshot;
import com.vitahealth.tata.adherenceanalytics.domain.repositories.AdherenceSnapshotRepository;
import com.vitahealth.tata.adherenceanalytics.infrastructure.persistence.jpa.assemblers.AdherenceSnapshotPersistenceAssembler;
import com.vitahealth.tata.adherenceanalytics.infrastructure.persistence.jpa.repositories.AdherenceSnapshotJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.Locale;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Adapts immutable snapshots while leaving atomic publication to application services. */
@Repository
public class AdherenceSnapshotRepositoryAdapter implements AdherenceSnapshotRepository {
  private final AdherenceSnapshotJpaRepository repository;
  private final EntityManager entities;

  /** Injects owned JPA storage and the caller-scoped persistence context. */
  public AdherenceSnapshotRepositoryAdapter(
      AdherenceSnapshotJpaRepository repository, EntityManager entities) {
    this.repository = repository;
    this.entities = entities;
  }

  /** Maps stored history in its read transaction. */
  @Override
  @Transactional(readOnly = true)
  public Optional<AdherencePeriodSnapshot> find(String id) {
    return repository.findById(id).map(AdherenceSnapshotPersistenceAssembler::toDomain);
  }

  /**
   * Flushes the new snapshot before synchronous publication without committing it independently.
   */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void insert(AdherencePeriodSnapshot snapshot) {
    try {
      entities.persist(AdherenceSnapshotPersistenceAssembler.toEntity(snapshot));
      entities.flush();
    } catch (RuntimeException failure) {
      if (isSnapshotIdentityCollision(failure))
        throw new AdherenceSnapshotAlreadyExistsException(failure);
      throw failure;
    }
  }

  private static boolean isSnapshotIdentityCollision(Throwable failure) {
    for (var cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation) {
        var name = violation.getConstraintName();
        if (!"23505".equals(violation.getSQLState()) || name == null) return false;
        var metadata = name.toUpperCase(Locale.ROOT);
        if (metadata.equals("ADHERENCE_PERIOD_SNAPSHOTS_PKEY")) return true;
        var sql = violation.getSQL();
        return sql != null
            && sql.toLowerCase(Locale.ROOT).startsWith("insert into adherence_period_snapshots (")
            && metadata.matches(
                "(?:PUBLIC\\.)?(?:CONSTRAINT_[0-9A-F]+ INDEX"
                    + " (?:PUBLIC\\.)?)?PRIMARY_KEY_[0-9A-F]+");
      }
    }
    return false;
  }
}
