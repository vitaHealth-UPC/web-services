package com.tata.familymonitoring.domain.ports;

import com.tata.familymonitoring.domain.model.valueobjects.IntakeSummary;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Port to Intake Execution. */
public interface IIntakeHistoryPort {

  List<IntakeSummary> getRecentIntakes(Long olderAdultId, int days);

  Optional<Instant> findNextIntakeAt(Long olderAdultId);
}
