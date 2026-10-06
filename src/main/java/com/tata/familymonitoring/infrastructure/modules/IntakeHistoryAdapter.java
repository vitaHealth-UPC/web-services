package com.tata.familymonitoring.infrastructure.modules;

import com.tata.familymonitoring.domain.model.valueobjects.IntakeSummary;
import com.tata.familymonitoring.domain.ports.IIntakeHistoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reads intake data from Intake Execution. That module does not expose its public contract yet, so
 * the adapter reports no data.
 */
@Component
public class IntakeHistoryAdapter implements IIntakeHistoryPort {

  @Override
  public List<IntakeSummary> getRecentIntakes(Long olderAdultId, int days) {
    return List.of();
  }

  @Override
  public Optional<Instant> findNextIntakeAt(Long olderAdultId) {
    return Optional.empty();
  }
}
