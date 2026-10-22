package com.vitahealth.tata.intakeexecution.domain.model.aggregates;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Keeps pending edits atomic and definitive intake evidence immutable. */
class IntakePendingRevisionTest {
  private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");
  private static final MedicationSnapshot ORIGINAL =
      new MedicationSnapshot("Original", "1 comprimido", "Original instructions");

  @Test
  void everyDefinitiveOutcomeRejectsMedicationRevisionWithoutChangingEvidence() {
    for (var status : List.of(IntakeStatus.CONFIRMED, IntakeStatus.LATE, IntakeStatus.OMITTED)) {
      var intake =
          Intake.rehydrate(
              "intake",
              "treatment",
              "medication",
              "adult",
              ORIGINAL,
              NOW,
              status,
              NOW.minusSeconds(3600),
              status == IntakeStatus.OMITTED ? null : NOW,
              status == IntakeStatus.OMITTED ? null : ConfirmationChannel.TOUCH,
              NOW.minusSeconds(30));
      assertThrows(
          IllegalStateException.class,
          () ->
              intake.revisePendingMedication(
                  "replacement",
                  new MedicationSnapshot("Replacement", "2 comprimidos", "Changed")));
      assertEquals("medication", intake.medicationId());
      assertEquals(ORIGINAL, intake.medication());
      assertEquals(status, intake.status());
      assertEquals(NOW.minusSeconds(3600), intake.createdAt());
      assertEquals(NOW.minusSeconds(30), intake.unconfirmedReportedAt());
    }
  }

  @Test
  void invalidPendingRevisionDoesNotPartiallyChangeTheMedicationReference() {
    var intake =
        Intake.createScheduled(
            "treatment", "medication", "adult", ORIGINAL, NOW, NOW.minusSeconds(3600));
    assertThrows(
        NullPointerException.class, () -> intake.revisePendingMedication("replacement", null));
    assertThrows(
        IllegalArgumentException.class, () -> intake.revisePendingMedication(" ", ORIGINAL));
    assertEquals("medication", intake.medicationId());
    assertEquals(ORIGINAL, intake.medication());
    assertTrue(intake.isPending());
  }
}
