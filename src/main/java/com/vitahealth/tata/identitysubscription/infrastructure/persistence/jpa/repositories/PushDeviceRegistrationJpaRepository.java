package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.PushDeviceRegistrationPersistenceEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persists versioned installation evidence and reserves existing rows for rotation. */
public interface PushDeviceRegistrationJpaRepository
    extends JpaRepository<PushDeviceRegistrationPersistenceEntity, String> {
  /** Reserves the existing installation; absent-row creation is protected by unique constraints. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select device from PushDeviceRegistrationPersistenceEntity device where"
          + " device.installationId = :id")
  Optional<PushDeviceRegistrationPersistenceEntity> findForUpdate(
      @Param("id") String installationId);

  /** Selects one stable page whose registered sessions match the requested subject and role. */
  @Query(
      "select device from PushDeviceRegistrationPersistenceEntity device where"
          + " device.installationId > :afterId and device.sessionId in (select session.id from"
          + " AccountSessionPersistenceEntity session where session.accountId = :subjectId and"
          + " session.role = :role and session.expiresAt > :now) order by device.installationId")
  List<PushDeviceRegistrationPersistenceEntity> findCandidates(
      @Param("subjectId") String subjectId,
      @Param("role") String role,
      @Param("afterId") String afterId,
      @Param("now") Instant now,
      Pageable page);
}
