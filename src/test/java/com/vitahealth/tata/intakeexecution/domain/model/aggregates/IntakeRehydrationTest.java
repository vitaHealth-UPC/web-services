package com.vitahealth.tata.intakeexecution.domain.model.aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Rejects ambiguous stored evidence without selecting a replacement time or channel. */
class IntakeRehydrationTest {
  private static final Instant AT = Instant.parse("2026-10-15T12:00:00Z");
  private static final MedicationSnapshot SNAPSHOT =
      new MedicationSnapshot("Losartán", "1 comprimido", "");

  @Test
  void restoresEachCompleteConfirmationExactlyAsStored() {
    for (var status : List.of(IntakeStatus.CONFIRMED, IntakeStatus.LATE)) {
      var recorded = status == IntakeStatus.LATE ? AT.plusSeconds(60) : AT;
      var intake = restore(status, recorded, ConfirmationChannel.VOICE);
      assertEquals(status, intake.status());
      assertEquals(recorded, intake.confirmedAt());
      assertEquals(ConfirmationChannel.VOICE, intake.confirmationChannel());
      assertEquals("intake", intake.id());
    }
  }

  @Test
  void rejectsConfirmedStatusWithMissingTimeOrChannelIncludingShortOverload() {
    for (var status : List.of(IntakeStatus.CONFIRMED, IntakeStatus.LATE)) {
      assertThrows(
          IllegalArgumentException.class, () -> restore(status, null, ConfirmationChannel.TOUCH));
      assertThrows(IllegalArgumentException.class, () -> restore(status, AT, null));
      assertThrows(
          IllegalArgumentException.class,
          () ->
              Intake.rehydrate(
                  "intake",
                  "treatment",
                  "medication",
                  "adult",
                  SNAPSHOT,
                  AT,
                  status,
                  AT.minusSeconds(60)));
    }
  }

  @Test
  void rejectsConfirmationMetadataOnPendingOrOmittedEvidence() {
    for (var status : List.of(IntakeStatus.PENDING, IntakeStatus.OMITTED)) {
      assertEquals(status, restore(status, null, null).status());
      assertThrows(IllegalArgumentException.class, () -> restore(status, AT, null));
      assertThrows(
          IllegalArgumentException.class, () -> restore(status, null, ConfirmationChannel.TOUCH));
      assertThrows(
          IllegalArgumentException.class, () -> restore(status, AT, ConfirmationChannel.TOUCH));
    }
  }

  private static Intake restore(
      IntakeStatus status, Instant confirmedAt, ConfirmationChannel channel) {
    return Intake.rehydrate(
        "intake",
        "treatment",
        "medication",
        "adult",
        SNAPSHOT,
        AT,
        status,
        AT.minusSeconds(60),
        confirmedAt,
        channel,
        AT.plusSeconds(120));
  }
}
