package com.tata.intakeexecution.infrastructure.persistence.jpa.repositories;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.tata.intakeexecution.infrastructure.persistence.jpa.entities.IntakePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IntakeJpaRepository extends JpaRepository<IntakePersistenceEntity, String> {
    List<IntakePersistenceEntity> findByOlderAdultIdAndStatusInAndScheduledAtGreaterThanEqualOrderByScheduledAtDesc(
            String olderAdultId, java.util.Collection<IntakeStatus> statuses, Instant from);
    Optional<IntakePersistenceEntity> findFirstByOlderAdultIdAndStatusOrderByScheduledAtAsc(
            String olderAdultId, IntakeStatus status);
    List<IntakePersistenceEntity> findTop200ByStatusAndUnconfirmedReportedAtIsNullAndScheduledAtLessThanEqualOrderByScheduledAtAscIdAsc(IntakeStatus status, Instant now);
    @Query("select i from IntakePersistenceEntity i where i.olderAdultId = :olderAdultId and i.scheduledAt >= :from and i.scheduledAt < :to order by i.scheduledAt, i.id")
    List<IntakePersistenceEntity> findAgenda(@Param("olderAdultId") String olderAdultId, @Param("from") Instant from, @Param("to") Instant to);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from IntakePersistenceEntity i where i.id = :id")
    Optional<IntakePersistenceEntity> findByIdForConfirmation(@Param("id") String id);

    List<IntakePersistenceEntity> findByTreatmentIdAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(
            String treatmentId,
            Instant from
    );

    Optional<IntakePersistenceEntity> findFirstByOlderAdultIdAndStatusAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(
            String olderAdultId,
            IntakeStatus status,
            Instant from
    );
}
