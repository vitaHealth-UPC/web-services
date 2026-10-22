package com.vitahealth.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.familymonitoring.application.internal.queryservices.FamilyMonitorSelectionService;
import com.vitahealth.tata.familymonitoring.application.internal.queryservices.GetCaregiverNotesQueryHandler;
import com.vitahealth.tata.familymonitoring.application.internal.queryservices.GetOlderAdultStatusQueryHandler;
import com.vitahealth.tata.familymonitoring.application.result.AdherenceInsightResult;
import com.vitahealth.tata.familymonitoring.application.result.AlertResult;
import com.vitahealth.tata.familymonitoring.application.result.LowStockNoticeResult;
import com.vitahealth.tata.familymonitoring.application.result.OlderAdultStatusResult;
import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.model.entities.AdherenceInsight;
import com.vitahealth.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.vitahealth.tata.familymonitoring.domain.model.entities.CaregiverNote;
import com.vitahealth.tata.familymonitoring.domain.model.entities.LowStockNotice;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetCaregiverNotesQuery;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetOlderAdultStatusQuery;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AdherenceSnapshot;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertStatus;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.OlderAdultStatus;
import com.vitahealth.tata.familymonitoring.domain.ports.AdherenceSummaryPort;
import com.vitahealth.tata.familymonitoring.domain.ports.IntakeHistoryPort;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Ensures application responses remain stable after domain transitions or caller mutation. */
class FamilyMonitoringResultIsolationTest {
  private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");

  @Test
  void equalTimeInterventionsUseTheSameIdentityTieBreakAsLatestNote() {
    var first = CaregiverNote.rehydrate(1L, "First", NOW, "caregiver");
    var second = CaregiverNote.rehydrate(2L, "Second", NOW, "caregiver");
    var monitor =
        FamilyMonitor.rehydrate(
            1L,
            "link",
            "adult",
            "caregiver",
            List.of(),
            List.of(first, second),
            List.of(),
            List.of(),
            NOW,
            NOW,
            0L);
    var selection = mock(FamilyMonitorSelectionService.class);
    when(selection.requireCurrent("adult", "caregiver")).thenReturn(monitor);
    var notes =
        new GetCaregiverNotesQueryHandler(selection)
            .handle(new GetCaregiverNotesQuery("adult", "caregiver"));
    assertEquals(List.of(2L, 1L), notes.stream().map(note -> note.id()).toList());
    assertEquals(monitor.latestNote().getId(), notes.getFirst().id());
  }

  @Test
  void capturedStatusCannotBeChangedThroughLaterAlertAndStockTransitions() {
    var alert =
        AlertSummary.rehydrate(1L, "intake", "Name", NOW, "Reason", AlertStatus.OPEN, NOW, null);
    var stock = LowStockNotice.rehydrate(2L, "medication", "Name", 3, 5, NOW);
    var monitor =
        FamilyMonitor.rehydrate(
            1L,
            "link",
            "adult",
            "caregiver",
            List.of(alert),
            List.of(),
            List.of(stock),
            List.of(),
            NOW,
            NOW,
            0L);
    var selection = mock(FamilyMonitorSelectionService.class);
    var history = mock(IntakeHistoryPort.class);
    var adherence = mock(AdherenceSummaryPort.class);
    when(selection.requireCurrent("adult", "caregiver")).thenReturn(monitor);
    when(history.getRecentIntakes("adult", 7)).thenReturn(List.of());
    when(history.findNextIntakeAt("adult")).thenReturn(Optional.empty());
    when(adherence.getWeeklySummary("adult")).thenReturn(new AdherenceSnapshot(0, 0));
    var query = new GetOlderAdultStatusQuery("adult", "caregiver");
    var handler = new GetOlderAdultStatusQueryHandler(selection, history, adherence);

    var captured = handler.handle(query);
    monitor.closeAlert(1L, NOW.plusSeconds(1));
    monitor.registerLowStock("medication", "Updated name", 1, 5, NOW.plusSeconds(1));
    var updated = handler.handle(query);

    assertEquals(AlertStatus.OPEN, captured.openAlerts().getFirst().status());
    assertEquals(3, captured.lowStockNotices().getFirst().remainingStock());
    assertEquals("Name", captured.lowStockNotices().getFirst().medicationName());
    assertEquals(0, updated.openAlerts().size());
    assertEquals(1, updated.lowStockNotices().getFirst().remainingStock());
    assertThrows(UnsupportedOperationException.class, () -> captured.openAlerts().clear());
  }

  @Test
  void statusCopiesAllCallerCollections() {
    var alerts =
        new ArrayList<>(
            List.of(
                new AlertResult(1L, "intake", "Name", NOW, "Reason", AlertStatus.OPEN, NOW, null)));
    var stock = new ArrayList<>(List.of(new LowStockNoticeResult("medication", "Name", 1, 5, NOW)));
    var day = LocalDate.of(2026, 10, 19);
    var insights =
        new ArrayList<>(
            List.of(
                AdherenceInsightResult.from(
                    AdherenceInsight.rehydrate(3L, "medication", "Name", 1, day, day, NOW))));
    var result =
        new OlderAdultStatusResult(
            new OlderAdultStatus(null, null, true),
            alerts,
            new AdherenceSnapshot(0, 0),
            stock,
            insights);

    alerts.clear();
    stock.clear();
    insights.clear();

    assertEquals(1, result.openAlerts().size());
    assertEquals(1, result.lowStockNotices().size());
    assertEquals(1, result.insights().size());
    assertThrows(UnsupportedOperationException.class, () -> result.lowStockNotices().clear());
    assertThrows(UnsupportedOperationException.class, () -> result.insights().clear());
  }
}
