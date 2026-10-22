package com.vitahealth.tata.familymonitoring.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AdherenceSnapshot;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.ContactChannel;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.ContactChannelType;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.IntakeSummary;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Protects summary values from impossible counters or missing source evidence. */
class FamilyMonitoringSummaryValuesTest {
  private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");

  @Test
  void validZeroEvidenceIsDistinctFromImpossibleAdherenceCounts() {
    assertEquals(0, new AdherenceSnapshot(0, 0).totalIntakes());
    assertEquals(4, new AdherenceSnapshot(4, 4).confirmedIntakes());
    for (int[] invalid : new int[][] {{-1, 4}, {0, -1}, {5, 4}, {1, 0}})
      assertThrows(
          IllegalArgumentException.class, () -> new AdherenceSnapshot(invalid[0], invalid[1]));
  }

  @Test
  void contactRequiresAChannelAndDestinationWithoutInventingAFormattingRule() {
    assertThrows(NullPointerException.class, () -> new ContactChannel(null, "+51 999 123 456"));
    assertThrows(
        IllegalArgumentException.class, () -> new ContactChannel(ContactChannelType.PHONE, " "));
    var channel = new ContactChannel(ContactChannelType.PHONE, " +51 999 123 456 ");
    assertEquals(" +51 999 123 456 ", channel.value());
  }

  @Test
  void intakeHistoryRequiresEvidenceAndPreservesTheOriginalSourceFields() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new IntakeSummary(" ", "Name", NOW, IntakeStatus.CONFIRMED));
    assertThrows(
        IllegalArgumentException.class,
        () -> new IntakeSummary("intake", " ", NOW, IntakeStatus.CONFIRMED));
    assertThrows(
        NullPointerException.class,
        () -> new IntakeSummary("intake", "Name", null, IntakeStatus.CONFIRMED));
    assertThrows(NullPointerException.class, () -> new IntakeSummary("intake", "Name", NOW, null));
    var summary = new IntakeSummary("intake", " Original name ", NOW, IntakeStatus.CONFIRMED);
    assertEquals(" Original name ", summary.medicationName());
    assertEquals(NOW, summary.scheduledAt());
  }
}
