package com.vitahealth.tata.intakeexecution.domain.model.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Validates reporting chronology independently of persistence and the scheduler clock. */
class ReportUnconfirmedIntakesCommandTest {
  @Test
  void acceptsEqualOrLaterReportingInstants() {
    var cutoff = Instant.parse("2026-10-15T12:00:00Z");
    assertEquals(cutoff, new ReportUnconfirmedIntakesCommand(cutoff, cutoff).reportedAt());
    assertEquals(
        cutoff.plusSeconds(1),
        new ReportUnconfirmedIntakesCommand(cutoff, cutoff.plusSeconds(1)).reportedAt());
  }

  @Test
  void rejectsMissingOrReversedReportingEvidence() {
    var now = Instant.parse("2026-10-15T12:00:00Z");
    assertThrows(
        IllegalArgumentException.class, () -> new ReportUnconfirmedIntakesCommand(null, now));
    assertThrows(
        IllegalArgumentException.class, () -> new ReportUnconfirmedIntakesCommand(now, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ReportUnconfirmedIntakesCommand(now, now.minusSeconds(1)));
  }
}
