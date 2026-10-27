package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.identitysubscription.domain.exceptions.DuplicatePushDeviceRegistration;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PushDeviceRegistration;
import com.vitahealth.tata.identitysubscription.domain.repositories.PushDeviceRegistrationRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.assemblers.PushDeviceRegistrationPersistenceAssembler;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.PushDeviceRegistrationPersistenceEntity;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.PushDeviceRegistrationJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Applies rotations to managed rows instead of merging detached ownership snapshots. */
@Repository
public class JpaPushDeviceRegistrationRepository implements PushDeviceRegistrationRepository {
  private final PushDeviceRegistrationJpaRepository rows;
  private final EntityManager entities;

  /** Injects storage and explicit refresh support for mutation reservations. */
  public JpaPushDeviceRegistrationRepository(
      PushDeviceRegistrationJpaRepository rows, EntityManager entities) {
    this.rows = rows;
    this.entities = entities;
  }

  /** Preserves session ownership and creation evidence when updating an installation address. */
  @Override
  @Transactional
  public PushDeviceRegistration save(PushDeviceRegistration value) {
    var existing = rows.findForUpdate(value.installationId());
    PushDeviceRegistrationPersistenceEntity managed;
    if (existing.isEmpty()) {
      managed = PushDeviceRegistrationPersistenceAssembler.toEntity(value);
      entities.persist(managed);
    } else {
      managed = existing.orElseThrow();
      entities.refresh(managed, LockModeType.PESSIMISTIC_WRITE);
      if (!managed.getSessionId().equals(value.sessionId())
          || !managed.getCreatedAt().equals(value.createdAt())
          || value.updatedAt().isBefore(managed.getUpdatedAt()))
        throw new DuplicatePushDeviceRegistration();
      managed.rotate(
          value.token(),
          PushDeviceRegistrationPersistenceAssembler.fingerprint(value.token()),
          value.updatedAt());
    }
    try {
      entities.flush();
    } catch (PersistenceException failure) {
      for (Throwable cause = failure; cause != null; cause = cause.getCause())
        if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState()))
          throw new DuplicatePushDeviceRegistration();
      throw failure;
    }
    return PushDeviceRegistrationPersistenceAssembler.toDomain(managed);
  }

  /** Reads an immutable registration snapshot inside its repository transaction. */
  @Override
  @Transactional(readOnly = true)
  public Optional<PushDeviceRegistration> findByInstallationId(String installationId) {
    return rows.findById(installationId).map(PushDeviceRegistrationPersistenceAssembler::toDomain);
  }

  /** Refreshes previously managed evidence under the caller's active row reservation. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<PushDeviceRegistration> findByInstallationIdForUpdate(String installationId) {
    return rows.findForUpdate(installationId)
        .map(
            row -> {
              entities.refresh(row, LockModeType.PESSIMISTIC_WRITE);
              return PushDeviceRegistrationPersistenceAssembler.toDomain(row);
            });
  }

  /** Removes only locked ownership; a new registration may be persisted after the removal flush. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void deleteOwned(String installationId, String sessionId) {
    var existing = rows.findForUpdate(installationId);
    if (existing.isEmpty()) return;
    var row = existing.orElseThrow();
    entities.refresh(row, LockModeType.PESSIMISTIC_WRITE);
    if (!row.getSessionId().equals(sessionId))
      throw new IllegalStateException("push device ownership changed before removal");
    entities.remove(row);
    entities.flush();
  }
}
