package com.tata.intakeexecution.infrastructure.persistence.jpa.adapters;

import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.tata.intakeexecution.infrastructure.persistence.jpa.repositories.IntakeJpaRepository;
import com.tata.intakeexecution.infrastructure.persistence.jpa.assemblers.IntakePersistenceAssembler;
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
    public List<Intake> findAgenda(String olderAdultId, Instant from, Instant to) {
        return repository.findAgenda(olderAdultId, from, to).stream().map(IntakePersistenceAssembler::toDomain).toList();
    }

    @Override
    public List<String> findOlderAdultIdsWithIntakes(Instant from, Instant to) {
        return repository.findOlderAdultIdsWithIntakes(from, to);
    }

    @Override
    public List<Intake> saveAll(List<Intake> intakes) {
        return repository.saveAll(intakes.stream().map(IntakePersistenceAssembler::toEntity).toList()).stream()
                .map(IntakePersistenceAssembler::toDomain)
                .toList();
    }

    @Override
    public Optional<Intake> findById(String id) {
        return repository.findById(id).map(IntakePersistenceAssembler::toDomain);
    }

    @Override
    public Optional<Intake> findByIdForConfirmation(String id) {
        return repository.findByIdForConfirmation(id).map(IntakePersistenceAssembler::toDomain);
    }

    @Override
    public List<Intake> findUnreportedPendingDue(Instant cutoff) {
        return repository
                .findTop200ByStatusAndUnconfirmedReportedAtIsNullAndScheduledAtLessThanEqualOrderByScheduledAtAscIdAsc(
                        IntakeStatus.PENDING,
                        cutoff
                )
                .stream()
                .map(IntakePersistenceAssembler::toDomain)
                .toList();
    }

    @Override
    public List<Intake> findFutureByTreatmentId(String treatmentId, Instant from) {
        return repository.findByTreatmentIdAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(treatmentId, from)
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
                .findFirstByOlderAdultIdAndStatusAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(
                        olderAdultId,
                        IntakeStatus.PENDING,
                        from
                )
                .map(IntakePersistenceAssembler::toDomain);
    }

}
