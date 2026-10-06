package com.tata.intakeexecution.domain.repositories;

import com.tata.intakeexecution.domain.model.aggregates.Intake;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IntakeRepository {
    List<Intake> saveAll(List<Intake> intakes);
    Optional<Intake> findById(String id);
    List<Intake> findFutureByTreatmentId(String treatmentId, Instant from);
    void deleteAll(List<Intake> intakes);
    Optional<Intake> findNextPendingByOlderAdultId(String olderAdultId, Instant from);
}
