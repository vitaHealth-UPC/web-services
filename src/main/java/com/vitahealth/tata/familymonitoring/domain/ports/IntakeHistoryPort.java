package com.vitahealth.tata.familymonitoring.domain.ports;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.IntakeSummary;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Reads exact-owner intake evidence after the caller has authorized caregiver access. */
public interface IntakeHistoryPort {

  /**
   * Returns completed outcomes in the requested elapsed-day window ending at the application clock.
   */
  List<IntakeSummary> getRecentIntakes(String olderAdultId, int days);

  /** Returns the source-selected next schedule, or absence when there is no next intake. */
  Optional<Instant> findNextIntakeAt(String olderAdultId);
}
