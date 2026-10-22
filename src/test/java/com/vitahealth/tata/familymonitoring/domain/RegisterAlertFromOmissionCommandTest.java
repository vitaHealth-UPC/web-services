package com.vitahealth.tata.familymonitoring.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.familymonitoring.domain.model.commands.RegisterAlertFromOmissionCommand;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Prevents incomplete source facts from entering relationship projection transactions. */
class RegisterAlertFromOmissionCommandTest {
  private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");

  @Test
  void rejectsMissingReferencesAndImpossibleOccurrenceTimes() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RegisterAlertFromOmissionCommand(
                " ", "intake", "Medication", NOW, "omission", NOW));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RegisterAlertFromOmissionCommand(
                "adult", null, "Medication", NOW, "omission", NOW));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RegisterAlertFromOmissionCommand(
                "adult", "intake", "Medication", NOW, "omission", null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RegisterAlertFromOmissionCommand(
                "adult", "intake", "Medication", NOW, "omission", NOW.minusSeconds(1)));
  }

  @Test
  void retainsOriginalOccurrenceWithoutUsingAReceiveTime() {
    var command =
        new RegisterAlertFromOmissionCommand(
            "adult", "intake", "Medication", NOW, "omission", NOW.plusSeconds(1800));
    assertEquals(NOW, command.scheduledAt());
    assertEquals(NOW.plusSeconds(1800), command.occurredAt());
  }
}
