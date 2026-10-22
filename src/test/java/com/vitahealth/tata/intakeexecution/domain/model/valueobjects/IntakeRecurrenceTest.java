package com.vitahealth.tata.intakeexecution.domain.model.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Checks the typed boundary between treatment snapshots and intake recurrence rules. */
class IntakeRecurrenceTest {
  @Test
  void retainsLegacyUtcAndNormalizesTheSupportedIntervalVocabulary() {
    assertEquals(
        new IntakeRecurrence(
            IntakeRecurrence.Mode.LOCAL_TIMES, new ScheduleTimeZone("UTC"), null, 0),
        IntakeRecurrence.fromSnapshot(null, null, null, null));
    var recurrence =
        IntakeRecurrence.fromSnapshot(
            "FIXED_INTERVAL", "America/Bogota", Instant.EPOCH, "  Cada  12 Horas ");
    assertEquals(12, recurrence.intervalHours());
    assertEquals(Instant.EPOCH, recurrence.anchor());
  }

  @Test
  void rejectsPartialUnsupportedAndImpreciseEvidence() {
    assertThrows(
        IllegalArgumentException.class,
        () -> IntakeRecurrence.fromSnapshot(null, "UTC", null, "DAILY"));
    assertThrows(
        IllegalArgumentException.class,
        () -> IntakeRecurrence.fromSnapshot("FIXED_INTERVAL", "UTC", null, "cada 8 horas"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            IntakeRecurrence.fromSnapshot("FIXED_INTERVAL", "UTC", Instant.EPOCH, "cada 6 horas"));
    assertThrows(
        IllegalArgumentException.class,
        () -> IntakeRecurrence.fromSnapshot("LOCAL_TIMES", "UTC", Instant.EPOCH, "DAILY"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            IntakeRecurrence.fromSnapshot(
                "FIXED_INTERVAL", "UTC", Instant.EPOCH.plusNanos(1), "cada 8 horas"));
  }
}
