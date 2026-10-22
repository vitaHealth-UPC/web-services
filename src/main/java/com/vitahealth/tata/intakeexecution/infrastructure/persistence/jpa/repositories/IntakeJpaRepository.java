package com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.entities.IntakePersistenceEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data queries for intake evidence and pessimistically locked transitions. */
public interface IntakeJpaRepository extends JpaRepository<IntakePersistenceEntity, String> {
  /** Reads the first unlocked reference page at or before the cutoff. */
  @Query(
      "select i.id as id, i.scheduledAt as scheduledAt from IntakePersistenceEntity i where"
          + " i.status = :status and i.unconfirmedReportedAt is null and i.scheduledAt <= :cutoff"
          + " order by i.scheduledAt, i.id")
  List<IntakeReportingCandidateProjection> findReportingCandidates(
      @Param("status") IntakeStatus status, @Param("cutoff") Instant cutoff, Pageable page);

  /**
   * Reads the continuation after a complete exclusive position, without nullable SQL parameters.
   */
  @Query(
      "select i.id as id, i.scheduledAt as scheduledAt from IntakePersistenceEntity i where"
          + " i.status = :status and i.unconfirmedReportedAt is null and i.scheduledAt <= :cutoff"
          + " and (i.scheduledAt > :afterAt or (i.scheduledAt = :afterAt and"
          + " i.id > :afterId)) order by i.scheduledAt, i.id")
  List<IntakeReportingCandidateProjection> findReportingCandidatesAfter(
      @Param("status") IntakeStatus status,
      @Param("cutoff") Instant cutoff,
      @Param("afterAt") Instant afterAt,
      @Param("afterId") String afterId,
      Pageable page);

  /** Reads a half-open agenda interval in stable schedule and identity order. */
  @Query(
      "select i from IntakePersistenceEntity i where i.olderAdultId = :olderAdultId and"
          + " i.scheduledAt >= :from and i.scheduledAt < :to order by i.scheduledAt, i.id")
  List<IntakePersistenceEntity> findAgenda(
      @Param("olderAdultId") String olderAdultId,
      @Param("from") Instant from,
      @Param("to") Instant to);

  /** Finds distinct adult references having evidence in the half-open interval. */
  @Query(
      "select distinct i.olderAdultId from IntakePersistenceEntity i where i.scheduledAt >= :from"
          + " and i.scheduledAt < :to and (:after is null or i.olderAdultId > :after) order by"
          + " i.olderAdultId")
  List<String> findOlderAdultIdsWithIntakes(
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("after") String after,
      Pageable page);

  /** Locks a confirmation source; the adapter refreshes any previously managed representation. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from IntakePersistenceEntity i where i.id = :id")
  Optional<IntakePersistenceEntity> findByIdForConfirmation(@Param("id") String id);

  /** Reads a treatment's slots from the inclusive scheduling boundary. */
  List<IntakePersistenceEntity>
      findByTreatmentIdAndScheduledAtGreaterThanEqualOrderByScheduledAtAscIdAsc(
          String treatmentId, Instant from);

  /** Finds the earliest matching slot, breaking simultaneous schedules by identifier. */
  Optional<IntakePersistenceEntity>
      findFirstByOlderAdultIdAndStatusAndScheduledAtGreaterThanEqualOrderByScheduledAtAscIdAsc(
          String olderAdultId, IntakeStatus status, Instant from);

  /** Serializes reconciliation with confirmation before slot status is materialized. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select i from IntakePersistenceEntity i where i.treatmentId = :treatmentId and i.scheduledAt"
          + " >= :from order by i.scheduledAt, i.id")
  List<IntakePersistenceEntity> findFutureByTreatmentIdForUpdate(
      @Param("treatmentId") String treatmentId, @Param("from") Instant from);
}
