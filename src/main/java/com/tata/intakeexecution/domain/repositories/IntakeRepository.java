package com.tata.intakeexecution.domain.repositories;

import com.tata.intakeexecution.domain.model.aggregates.Intake;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IntakeRepository {
    List<Intake> findAgenda(String olderAdultId, Instant from, Instant to);
    List<Intake> saveAll(List<Intake> intakes);
    Optional<Intake> findById(String id);
    default Optional<Intake> findByIdForConfirmation(String id) { return findById(id); }
    List<Intake> findFutureByTreatmentId(String treatmentId, Instant from);
    void deleteAll(List<Intake> intakes);
    Optional<Intake> findNextPendingByOlderAdultId(String olderAdultId, Instant from);
}
