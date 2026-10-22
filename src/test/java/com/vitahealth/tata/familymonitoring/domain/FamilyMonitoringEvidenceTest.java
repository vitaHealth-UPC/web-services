package com.vitahealth.tata.familymonitoring.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.vitahealth.tata.familymonitoring.domain.model.entities.CaregiverNote;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Checks that invalid intervention and omission evidence cannot alter valid follow-up history. */
class FamilyMonitoringEvidenceTest {
  private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");

  private AlertSummary alert() {
    return new AlertSummary("intake", "Losartan", NOW.minusSeconds(60), "Omitted", NOW);
  }

  @Test
  void failedClosureLeavesOpenAndAttendedStatesUntouched() {
    for (var attended : new boolean[] {false, true}) {
      var alert = alert();
      if (attended) alert.markAttended();
      var before = alert.getStatus();
      assertThrows(NullPointerException.class, () -> alert.close(null));
      assertThrows(IllegalArgumentException.class, () -> alert.close(NOW.minusNanos(1)));
      assertEquals(before, alert.getStatus());
      assertNull(alert.getClosedAt());
      alert.close(NOW);
      assertEquals(AlertStatus.CLOSED, alert.getStatus());
      assertEquals(NOW, alert.getClosedAt());
    }
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  void rejectsMissingSourceEvidenceBeforeAddingAnAlert(String missing) {
    var monitor = new FamilyMonitor("link", "adult", "caregiver");
    assertThrows(
        IllegalArgumentException.class,
        () -> monitor.addAlert(missing, "Losartan", NOW, "Omitted", NOW));
    assertThrows(
        IllegalArgumentException.class,
        () -> monitor.addAlert("intake", missing, NOW, "Omitted", NOW));
    assertThrows(
        IllegalArgumentException.class,
        () -> monitor.addAlert("intake", "Losartan", NOW, missing, NOW));
    assertTrue(monitor.getAlerts().isEmpty());
  }

  @Test
  void creationAndRestorationRejectOpeningBeforeTheOmittedSchedule() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new AlertSummary("intake", "Losartan", NOW.plusNanos(1), "Omitted", NOW));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AlertSummary.rehydrate(
                1L,
                "intake",
                "Losartan",
                NOW.plusNanos(1),
                "Omitted",
                AlertStatus.OPEN,
                NOW,
                null));
  }

  @ParameterizedTest
  @EnumSource(AlertStatus.class)
  void restoresOriginalEvidenceOnlyWhenClosureMatchesTheStoredState(AlertStatus status) {
    var closedAt = status == AlertStatus.CLOSED ? NOW.plusSeconds(10) : null;
    var restored =
        AlertSummary.rehydrate(
            7L,
            "intake",
            " Losartan ",
            NOW.minusSeconds(60),
            " Original reason ",
            status,
            NOW,
            closedAt);
    assertEquals(7L, restored.getId());
    assertEquals(" Losartan ", restored.getMedicationName());
    assertEquals(" Original reason ", restored.getReason());
    assertEquals(status, restored.getStatus());
    assertEquals(closedAt, restored.getClosedAt());
    var inconsistent = status == AlertStatus.CLOSED ? null : NOW;
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AlertSummary.rehydrate(
                7L, "intake", "Losartan", NOW, "Omitted", status, NOW, inconsistent));
  }

  @Test
  void restorationRejectsInvalidStoredIdentitiesAndClosureChronology() {
    for (Long id : new Long[] {null, 0L, -1L}) {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              AlertSummary.rehydrate(
                  id, "intake", "Losartan", NOW, "Omitted", AlertStatus.OPEN, NOW, null));
      assertThrows(
          IllegalArgumentException.class,
          () -> CaregiverNote.rehydrate(id, "Note", NOW, "caregiver"));
    }
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AlertSummary.rehydrate(
                1L,
                "intake",
                "Losartan",
                NOW,
                "Omitted",
                AlertStatus.CLOSED,
                NOW,
                NOW.minusNanos(1)));
  }

  @Test
  void restoredNotesPreserveHistoricTextAndRejectInvalidEvidence() {
    var restored = CaregiverNote.rehydrate(1L, " Original note ", NOW, "caregiver");
    assertEquals(" Original note ", restored.getText());
    assertEquals(NOW, restored.getRecordedAt());
    assertThrows(
        IllegalArgumentException.class, () -> CaregiverNote.rehydrate(1L, " ", NOW, "caregiver"));
    assertThrows(
        IllegalArgumentException.class,
        () -> CaregiverNote.rehydrate(1L, "x".repeat(1001), NOW, "caregiver"));
    assertThrows(
        IllegalArgumentException.class, () -> CaregiverNote.rehydrate(1L, "Note", NOW, " "));
    assertThrows(
        NullPointerException.class, () -> CaregiverNote.rehydrate(1L, "Note", null, "caregiver"));
  }

  @Test
  void anotherCaregiverCannotAppendAnInterventionToTheRelationship() {
    var monitor = new FamilyMonitor("link", "adult", "caregiver");
    assertThrows(
        IllegalArgumentException.class,
        () -> monitor.addNote("Foreign intervention", "another-caregiver", NOW));
    assertTrue(monitor.getNotes().isEmpty());
    monitor.addNote(" Own intervention ", "caregiver", NOW);
    assertEquals("Own intervention", monitor.getNotes().getFirst().getText());
  }
}
