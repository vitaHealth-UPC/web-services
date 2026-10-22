package com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationIntentState;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.NotificationIntentPersistenceEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Stores unique source intentions and exposes bounded work selection plus explicit reservations.
 */
public interface NotificationIntentJpaRepository
    extends JpaRepository<NotificationIntentPersistenceEntity, String> {
  /** Reads an idempotent intention while the caller holds its source-case reservation. */
  Optional<NotificationIntentPersistenceEntity> findBySourceCaseIdAndKind(
      long sourceCaseId, NotificationKind kind);

  /** Reserves one intention before changing its lease or fanout progress. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select intent from NotificationIntentPersistenceEntity intent where intent.id = :id")
  Optional<NotificationIntentPersistenceEntity> findForUpdate(@Param("id") String id);

  /** Selects scalar IDs only; one candidate cannot hold unbounded clinical histories in memory. */
  @Query(
      "select intent.id from NotificationIntentPersistenceEntity intent where intent.state = :ready"
          + " and intent.nextAttemptAt <= :now and (intent.leaseUntil is null or intent.leaseUntil"
          + " <= :now) order by intent.nextAttemptAt, intent.id")
  List<String> findDueIds(
      @Param("ready") NotificationIntentState ready, @Param("now") Instant now, Pageable page);

  /** Scans due fanout IDs in keyset order so a failing earlier case cannot starve later work. */
  @Query(
      "select intent.id from NotificationIntentPersistenceEntity intent where intent.id > :afterId"
          + " and intent.state = :ready and intent.nextAttemptAt <= :now and (intent.leaseUntil is"
          + " null or intent.leaseUntil <= :now) order by intent.id")
  List<String> findDueIdsAfter(
      @Param("afterId") String afterId,
      @Param("ready") NotificationIntentState ready,
      @Param("now") Instant now,
      Pageable page);

  /** Selects completed fanout awaiting source completion, independently of delivery retry state. */
  @Query(
      "select intent.id from NotificationIntentPersistenceEntity intent where intent.id > :afterId"
          + " and intent.state = :state order by intent.id")
  List<String> findStateIdsAfter(
      @Param("afterId") String afterId,
      @Param("state") NotificationIntentState state,
      Pageable page);
}
