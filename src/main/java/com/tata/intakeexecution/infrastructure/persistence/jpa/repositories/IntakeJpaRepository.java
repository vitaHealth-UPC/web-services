package com.tata.intakeexecution.infrastructure.persistence.jpa.repositories;

import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.tata.intakeexecution.infrastructure.persistence.jpa.entities.IntakePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IntakeJpaRepository extends JpaRepository<IntakePersistenceEntity, String> {
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
