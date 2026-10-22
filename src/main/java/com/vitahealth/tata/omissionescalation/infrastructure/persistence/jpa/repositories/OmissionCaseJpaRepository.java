package com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.OmissionCasePersistenceEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Reads omission rows and bounded recovery references, reserving mutations explicitly. */
public interface OmissionCaseJpaRepository
    extends JpaRepository<OmissionCasePersistenceEntity, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from OmissionCasePersistenceEntity c where c.id = :id")
  Optional<OmissionCasePersistenceEntity> findByIdForUpdate(@Param("id") Long id);

  Optional<OmissionCasePersistenceEntity> findByIntakeId(String intakeId);

  /** Selects expired references without reconstructing potentially corrupt case history. */
  @Query(
      "select c.id from OmissionCasePersistenceEntity c where c.id > :afterId and c.status ="
          + " :status and c.gracePeriod.endsAt <= :now order by c.id")
  List<Long> findExpiredPendingIdsAfter(
      @Param("status") OmissionCaseStatus status,
      @Param("now") Instant now,
      @Param("afterId") long afterId,
      Pageable page);

  /** Selects references only; the handler rechecks active state under its row lock. */
  @Query(
      "select c.id from OmissionCasePersistenceEntity c where c.id > :afterId and c.status in"
          + " :statuses and c.alerts is empty order by c.id")
  List<Long> findActiveCaseIdsWithoutAlertAfter(
      @Param("afterId") long afterId,
      @Param("statuses") Collection<OmissionCaseStatus> statuses,
      Pageable page);

  /** Selects active references for isolated escalation decisions. */
  @Query(
      "select c.id from OmissionCasePersistenceEntity c where c.id > :afterId and c.status in"
          + " :statuses order by c.id")
  List<Long> findActiveCaseIdsAfter(
      @Param("statuses") Collection<OmissionCaseStatus> statuses,
      @Param("afterId") long afterId,
      Pageable page);

  /** Recovers unsent pending reminders that predate durable notification intentions. */
  @Query(
      "select c.id from OmissionCasePersistenceEntity c where c.id > :afterId and c.status ="
          + " :pending and c.reinforcedReminderSentAt is null and not exists (select intent.id from"
          + " NotificationIntentPersistenceEntity intent where intent.sourceCase = c and"
          + " intent.kind = :kind) order by c.id")
  List<Long> findMissingReminderIntentIdsAfter(
      @Param("afterId") long afterId,
      @Param("pending") OmissionCaseStatus pending,
      @Param("kind") NotificationKind kind,
      Pageable page);

  /** Recovers active unsent caregiver alerts while retaining their original business evidence. */
  @Query(
      "select c.id from OmissionCasePersistenceEntity c where c.id > :afterId and c.status in"
          + " :active and not exists (select alert.id from OmissionCasePersistenceEntity owner join"
          + " owner.alerts alert where owner = c and alert.status = :sent) and not exists (select"
          + " intent.id from NotificationIntentPersistenceEntity intent where intent.sourceCase = c"
          + " and intent.kind = :kind) order by c.id")
  List<Long> findMissingAlertIntentIdsAfter(
      @Param("afterId") long afterId,
      @Param("active") Collection<OmissionCaseStatus> active,
      @Param("sent") AlertStatus sent,
      @Param("kind") NotificationKind kind,
      Pageable page);
}
