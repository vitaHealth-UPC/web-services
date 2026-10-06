package com.tata.familymonitoring.infrastructure.modules;

import com.tata.familymonitoring.domain.model.valueobjects.IntakeSummary;
import com.tata.familymonitoring.domain.model.valueobjects.IntakeStatus;
import com.tata.familymonitoring.domain.ports.IIntakeHistoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.tata.intakeexecution.infrastructure.persistence.jpa.entities.IntakePersistenceEntity;
import com.tata.intakeexecution.infrastructure.persistence.jpa.repositories.IntakeJpaRepository;
import org.springframework.stereotype.Component;

/**
 * Reads intake data from Intake Execution. That module does not expose its public contract yet, so
 * the adapter reports no data.
 */
@Component
public class IntakeHistoryAdapter implements IIntakeHistoryPort {
  private final IntakeJpaRepository intakeRepository;

  public IntakeHistoryAdapter(IntakeJpaRepository intakeRepository) {
    this.intakeRepository = intakeRepository;
  }

  @Override
  public List<IntakeSummary> getRecentIntakes(String olderAdultId, int days) {
    var from = Instant.now().minus(java.time.Duration.ofDays(days));
    return intakeRepository.findByOlderAdultIdAndStatusInAndScheduledAtGreaterThanEqualOrderByScheduledAtDesc(
        olderAdultId, java.util.List.of(com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus.CONFIRMED, com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus.LATE, com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus.OMITTED), from)
        .stream().map(IntakeHistoryAdapter::toSummary).toList();
  }

  @Override
  public Optional<Instant> findNextIntakeAt(String olderAdultId) {
    return intakeRepository.findFirstByOlderAdultIdAndStatusOrderByScheduledAtAsc(
        olderAdultId, com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus.PENDING).map(IntakePersistenceEntity::getScheduledAt);
  }

  private static IntakeSummary toSummary(IntakePersistenceEntity intake) {
    return new IntakeSummary(intake.getId(), intake.getMedicationName(), intake.getScheduledAt(),
        IntakeStatus.valueOf(intake.getStatus().name()));
  }
}

