package com.vitahealth.tata.familymonitoring.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.model.entities.AdherenceInsight;
import com.vitahealth.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.vitahealth.tata.familymonitoring.domain.model.entities.CaregiverNote;
import com.vitahealth.tata.familymonitoring.domain.model.entities.LowStockNotice;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Protects aggregate restoration from malformed stored identity, history and audit evidence. */
class FamilyMonitorRestorationTest {
  private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");

  private FamilyMonitor restore(
      Long id,
      Long revision,
      Instant created,
      Instant updated,
      List<AlertSummary> alerts,
      List<CaregiverNote> notes,
      List<LowStockNotice> stock,
      List<AdherenceInsight> insights) {
    return FamilyMonitor.rehydrate(
        id,
        "link",
        "adult",
        "caregiver",
        alerts,
        notes,
        stock,
        insights,
        created,
        updated,
        revision);
  }

  private FamilyMonitor withNotes(List<CaregiverNote> notes) {
    return restore(1L, 0L, NOW, NOW, List.of(), notes, List.of(), List.of());
  }

  @Test
  void requiresStoredIdentityRevisionAndOrderedAuditInstants() {
    for (Long id : new Long[] {null, 0L, -1L})
      assertThrows(
          IllegalArgumentException.class,
          () -> restore(id, 0L, NOW, NOW, List.of(), List.of(), List.of(), List.of()));
    for (Long revision : new Long[] {null, -1L})
      assertThrows(
          IllegalArgumentException.class,
          () -> restore(1L, revision, NOW, NOW, List.of(), List.of(), List.of(), List.of()));
    assertThrows(
        NullPointerException.class,
        () -> restore(1L, 0L, null, NOW, List.of(), List.of(), List.of(), List.of()));
    assertThrows(
        NullPointerException.class,
        () -> restore(1L, 0L, NOW, null, List.of(), List.of(), List.of(), List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(1L, 0L, NOW, NOW.minusNanos(1), List.of(), List.of(), List.of(), List.of()));
  }

  @Test
  void rejectsMissingUnsavedDuplicatedAndForeignAuthorNotes() {
    var note = CaregiverNote.rehydrate(2L, "Original", NOW, "caregiver");
    assertThrows(NullPointerException.class, () -> withNotes(null));
    assertThrows(NullPointerException.class, () -> withNotes(Arrays.asList(note, null)));
    assertThrows(
        IllegalArgumentException.class,
        () -> withNotes(List.of(new CaregiverNote("Unsaved", "caregiver", NOW))));
    assertThrows(IllegalArgumentException.class, () -> withNotes(List.of(note, note)));
    assertThrows(
        IllegalArgumentException.class,
        () -> withNotes(List.of(CaregiverNote.rehydrate(3L, "Foreign", NOW, "other-caregiver"))));
  }

  @Test
  void rejectsDistinctStoredRowsThatDuplicateTheSameLogicalSource() {
    var first =
        AlertSummary.rehydrate(1L, "intake", "Name", NOW, "Reason", AlertStatus.OPEN, NOW, null);
    var second =
        AlertSummary.rehydrate(2L, "intake", "Name", NOW, "Reason", AlertStatus.OPEN, NOW, null);
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(1L, 0L, NOW, NOW, List.of(first, second), List.of(), List.of(), List.of()));
    var stock1 = LowStockNotice.rehydrate(1L, "medication", "Name", 1, 5, NOW);
    var stock2 = LowStockNotice.rehydrate(2L, "medication", "Name", 2, 5, NOW);
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(1L, 0L, NOW, NOW, List.of(), List.of(), List.of(stock1, stock2), List.of()));
    var day = LocalDate.of(2026, 10, 18);
    var insight1 = AdherenceInsight.rehydrate(1L, "medication", "Name", 1, day, day, NOW);
    var insight2 = AdherenceInsight.rehydrate(2L, "medication", "Name", 1, day, day, NOW);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            restore(
                1L, 0L, NOW, NOW, List.of(), List.of(), List.of(), List.of(insight1, insight2)));
  }

  @Test
  void copiesCallerCollectionsWithoutRewritingOriginalClinicalTimesOrText() {
    var note = CaregiverNote.rehydrate(2L, " Original ", NOW.minusSeconds(60), "caregiver");
    var callerNotes = new ArrayList<>(List.of(note));
    var restored = withNotes(callerNotes);
    callerNotes.clear();
    assertEquals(1, restored.getNotes().size());
    assertEquals(" Original ", restored.getNotes().getFirst().getText());
    assertEquals(NOW.minusSeconds(60), restored.getNotes().getFirst().getRecordedAt());
    assertThrows(UnsupportedOperationException.class, () -> restored.getNotes().clear());
  }
}
