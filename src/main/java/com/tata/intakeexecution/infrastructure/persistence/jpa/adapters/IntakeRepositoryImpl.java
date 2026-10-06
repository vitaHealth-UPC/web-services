package com.tata.intakeexecution.infrastructure.persistence.jpa.adapters;

import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.tata.intakeexecution.infrastructure.persistence.jpa.entities.IntakePersistenceEntity;
import com.tata.intakeexecution.infrastructure.persistence.jpa.repositories.IntakeJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class IntakeRepositoryImpl implements IntakeRepository {
    private final IntakeJpaRepository repository;

    public IntakeRepositoryImpl(IntakeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Intake> saveAll(List<Intake> intakes) {
        return repository.saveAll(intakes.stream().map(this::toEntity).toList()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Intake> findFutureByTreatmentId(String treatmentId, Instant from) {
        return repository.findByTreatmentIdAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(treatmentId, from)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteAll(List<Intake> intakes) {
        repository.deleteAll(intakes.stream().map(this::toEntity).toList());
    }

    @Override
    public Optional<Intake> findNextPendingByOlderAdultId(String olderAdultId, Instant from) {
        return repository
                .findFirstByOlderAdultIdAndStatusAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(
                        olderAdultId,
                        IntakeStatus.PENDING,
                        from
                )
                .map(this::toDomain);
    }

    private Intake toDomain(IntakePersistenceEntity entity) {
        return Intake.rehydrate(
                entity.getId(),
                entity.getTreatmentId(),
                entity.getMedicationId(),
                entity.getOlderAdultId(),
                new MedicationSnapshot(
                        entity.getMedicationName(),
                        entity.getDose(),
                        entity.getInstructions()
                ),
                entity.getScheduledAt(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    private IntakePersistenceEntity toEntity(Intake intake) {
        return new IntakePersistenceEntity(
                intake.id(),
                intake.treatmentId(),
                intake.medicationId(),
                intake.olderAdultId(),
                intake.medication().name(),
                intake.medication().dose(),
                intake.medication().instructions(),
                intake.scheduledAt(),
                intake.status(),
                intake.createdAt()
        );
    }
}
