package com.vitahealth.tata.intakeexecution.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeScheduleChangeSet;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ScheduledIntakeSlot;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies schedule decisions preserve evidence and respect an explicit horizon. */
class IntakeScheduleReconciliationPolicyTest {
  private static final Instant FROM = Instant.parse("2026-10-15T08:00:00Z");
  private static final Instant TO = FROM.plus(7, ChronoUnit.DAYS);
  private static final MedicationSnapshot ORIGINAL =
      new MedicationSnapshot("Original", "1 comprimido", "Original");
  private static final MedicationSnapshot UPDATED =
      new MedicationSnapshot("Actualizado", "2 comprimidos", "Actualizado");

  @Test
  void retainsIdentityCreationAndReportingMarkerWithoutMutatingInput() {
    var at = FROM.plus(1, ChronoUnit.HOURS);
    var registered = FROM.minus(1, ChronoUnit.DAYS);
    var reported = FROM.minusSeconds(1);
    var original =
        Intake.rehydrate(
            "persistent-id",
            "treatment",
            "old-medication",
            "adult",
            ORIGINAL,
            at,
            IntakeStatus.PENDING,
            registered,
            null,
            null,
            reported);
    var result = reconcile(List.of(original), List.of(slot(at)));
    var revised = result.pending().getFirst();
    assertNotSame(original, revised);
    assertEquals("persistent-id", revised.id());
    assertEquals(registered, revised.createdAt());
    assertEquals(reported, revised.unconfirmedReportedAt());
    assertEquals(at, revised.scheduledAt());
    assertEquals("new-medication", revised.medicationId());
    assertEquals(UPDATED, revised.medication());
    assertEquals(ORIGINAL, original.medication());
    assertEquals("old-medication", original.medicationId());
    assertTrue(result.cancelled().isEmpty());
  }

  @Test
  void neverRevisesOrCancelsAnyDefinitiveOutcome() {
    for (var status : List.of(IntakeStatus.CONFIRMED, IntakeStatus.LATE, IntakeStatus.OMITTED)) {
      var at = FROM.plus(1, ChronoUnit.HOURS);
      var confirmed = status == IntakeStatus.OMITTED ? null : FROM;
      var channel = status == IntakeStatus.OMITTED ? null : ConfirmationChannel.TOUCH;
      var original =
          Intake.rehydrate(
              "definitive",
              "treatment",
              "old-medication",
              "adult",
              ORIGINAL,
              at,
              status,
              FROM.minusSeconds(100),
              confirmed,
              channel);
      var result = reconcile(List.of(original), List.of(slot(at)));
      assertTrue(result.pending().isEmpty());
      assertTrue(result.cancelled().isEmpty());
      assertEquals(status, original.status());
      assertEquals(confirmed, original.confirmedAt());
      assertEquals(channel, original.confirmationChannel());
      assertEquals(ORIGINAL, original.medication());
      assertTrue(reconcile(List.of(original), List.of()).cancelled().isEmpty());
    }
  }

  @Test
  void cancelsOnlyObsoletePendingInsideHalfOpenHorizon() {
    var past = pending("past", FROM.minusSeconds(1));
    var first = pending("first", FROM);
    var last = pending("last", TO.minusSeconds(1));
    var beyond = pending("beyond", TO);
    var result = reconcile(List.of(beyond, last, past, first), List.of());
    assertEquals(List.of("first", "last"), result.cancelled().stream().map(Intake::id).toList());
    assertTrue(result.pending().isEmpty());
    assertTrue(past.isPending());
    assertTrue(beyond.isPending());
  }

  @Test
  void createsMissingSlotsInChronologicalOrderAndReplayKeepsTheirIds() {
    var first = slot(FROM);
    var last = slot(TO.minusSeconds(1));
    var created = reconcile(List.of(), List.of(last, first));
    assertEquals(
        List.of(FROM, TO.minusSeconds(1)),
        created.pending().stream().map(Intake::scheduledAt).toList());
    var replay = reconcile(created.pending(), List.of(first, last));
    assertEquals(
        created.pending().stream().map(Intake::id).toList(),
        replay.pending().stream().map(Intake::id).toList());
    assertTrue(replay.cancelled().isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> created.pending().clear());
    assertThrows(UnsupportedOperationException.class, () -> created.cancelled().clear());
  }

  @Test
  void rejectsAmbiguousStoredOrExpectedSlotsWithoutChoosingAWinner() {
    var existing = pending("one", FROM);
    assertThrows(
        IllegalStateException.class,
        () -> reconcile(List.of(existing, pending("two", FROM)), List.of(slot(FROM))));
    assertThrows(
        IllegalArgumentException.class,
        () -> reconcile(List.of(existing), List.of(slot(FROM), slot(FROM))));
    assertEquals(ORIGINAL, existing.medication());
  }

  @Test
  void rejectsCrossOwnerEvidenceAndOutOfHorizonExpectations() {
    var other =
        Intake.createScheduled("treatment", "medication", "other-adult", ORIGINAL, FROM, FROM);
    assertThrows(IllegalStateException.class, () -> reconcile(List.of(other), List.of()));
    assertThrows(IllegalArgumentException.class, () -> reconcile(List.of(), List.of(slot(TO))));
    assertThrows(
        IllegalArgumentException.class,
        () -> reconcile(List.of(), List.of(slot(FROM.minusSeconds(1)))));
  }

  private static Intake pending(String id, Instant at) {
    return Intake.rehydrate(
        id,
        "treatment",
        "old-medication",
        "adult",
        ORIGINAL,
        at,
        IntakeStatus.PENDING,
        FROM.minusSeconds(100));
  }

  private static ScheduledIntakeSlot slot(Instant at) {
    return new ScheduledIntakeSlot("treatment", "adult", "new-medication", UPDATED, at);
  }

  private static IntakeScheduleChangeSet reconcile(
      List<Intake> existing, List<ScheduledIntakeSlot> expected) {
    return IntakeScheduleReconciliationPolicy.reconcile(
        "treatment", "adult", existing, expected, FROM, TO);
  }
}
