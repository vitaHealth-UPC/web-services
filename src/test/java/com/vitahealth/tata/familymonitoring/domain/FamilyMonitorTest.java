package com.vitahealth.tata.familymonitoring.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.familymonitoring.domain.exceptions.AlertNotFoundException;
import com.vitahealth.tata.familymonitoring.domain.exceptions.InvalidAlertTransitionException;
import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.vitahealth.tata.familymonitoring.domain.model.entities.CaregiverNote;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class FamilyMonitorTest {

  private static final Instant NOW = Instant.parse("2026-10-05T14:00:00Z");

  private FamilyMonitor monitor() {
    return new FamilyMonitor("link-1", "adult-10", "account-5");
  }

  private AlertSummary addAlert(FamilyMonitor monitor, String intakeId) {
    return monitor.addAlert(
        intakeId, "Losartan 50 mg", NOW.minusSeconds(1800), "Not confirmed", NOW);
  }

  @Test
  void addAlert_startsOpen() {
    FamilyMonitor monitor = monitor();

    addAlert(monitor, "intake-100");

    assertThat(monitor.hasPendingAlerts()).isTrue();
    assertThat(monitor.pendingAlerts()).hasSize(1);
  }

  @Test
  void addAlert_forTheSameIntake_doesNotDuplicate() {
    FamilyMonitor monitor = monitor();

    addAlert(monitor, "intake-100");
    addAlert(monitor, "intake-100");

    assertThat(monitor.getAlerts()).hasSize(1);
  }

  @Test
  void markAttended_thenClose_keepsTheAlertInTheHistory() {
    var alert =
        AlertSummary.rehydrate(
            1L, "intake-100", "Losartan", NOW, "Omitted", AlertStatus.OPEN, NOW, null);
    var monitor =
        FamilyMonitor.rehydrate(
            1L,
            "link",
            "adult",
            "caregiver",
            List.of(alert),
            List.of(),
            List.of(),
            List.of(),
            NOW,
            NOW,
            0L);
    monitor.markAlertAttended(1L);
    assertThat(monitor.hasPendingAlerts()).isTrue();
    assertThat(monitor.pendingAlerts().getFirst().getStatus()).isEqualTo(AlertStatus.ATTENDED);
    monitor.closeAlert(1L, NOW.plusSeconds(60));
    assertThat(monitor.findAlert(1L).getStatus()).isEqualTo(AlertStatus.CLOSED);
    assertThat(monitor.findAlert(1L).getClosedAt()).isEqualTo(NOW.plusSeconds(60));
    assertThat(monitor.hasPendingAlerts()).isFalse();
    assertThat(monitor.getAlerts()).hasSize(1);
  }

  @Test
  void close_aClosedAlert_isRejected() {
    AlertSummary alert = addAlert(monitor(), "intake-100");
    alert.close(NOW);

    assertThatThrownBy(() -> alert.close(NOW)).isInstanceOf(InvalidAlertTransitionException.class);
  }

  @Test
  void markAttended_aClosedAlert_isRejected() {
    AlertSummary alert = addAlert(monitor(), "intake-100");
    alert.close(NOW);

    assertThatThrownBy(alert::markAttended).isInstanceOf(InvalidAlertTransitionException.class);
  }

  @Test
  void findAlert_unknownId_throwsNotFound() {
    assertThatThrownBy(() -> monitor().findAlert(99L)).isInstanceOf(AlertNotFoundException.class);
  }

  @Test
  void addNote_storesTextAuthorAndTime() {
    FamilyMonitor monitor = monitor();

    monitor.addNote("  Called her, all good  ", "account-5", NOW);

    assertThat(monitor.latestNote().getText()).isEqualTo("Called her, all good");
    assertThat(monitor.latestNote().getFamiliarId()).isEqualTo("account-5");
    assertThat(monitor.latestNote().getRecordedAt()).isEqualTo(NOW);
  }

  @Test
  void addNote_blankText_isRejected() {
    assertThatThrownBy(() -> monitor().addNote("   ", "account-5", NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void latestNoteDoesNotDependOnPersistenceCollectionOrder() {
    var newest = CaregiverNote.rehydrate(2L, "Newest", NOW, "account-5");
    var older = CaregiverNote.rehydrate(1L, "Older", NOW.minusSeconds(60), "account-5");
    var restored =
        FamilyMonitor.rehydrate(
            1L,
            "link-1",
            "adult-10",
            "account-5",
            List.of(),
            List.of(newest, older),
            List.of(),
            List.of(),
            NOW,
            NOW,
            0L);
    assertThat(restored.latestNote()).isSameAs(newest);
  }

  @Test
  void latestNoteBreaksEqualTimeTiesByPersistedIdentity() {
    var second = CaregiverNote.rehydrate(2L, "Second", NOW, "account-5");
    var first = CaregiverNote.rehydrate(1L, "First", NOW, "account-5");
    var restored =
        FamilyMonitor.rehydrate(
            1L,
            "link-1",
            "adult-10",
            "account-5",
            List.of(),
            List.of(second, first),
            List.of(),
            List.of(),
            NOW,
            NOW,
            0L);
    assertThat(restored.latestNote()).isSameAs(second);
  }

  @Test
  void rejectsMissingRelationshipIdentityAndNoteTimeBeforeMutation() {
    assertThatThrownBy(() -> new FamilyMonitor(" ", "adult-10", "account-5"))
        .isInstanceOf(IllegalArgumentException.class);
    var monitor = monitor();
    assertThatThrownBy(() -> monitor.addNote("Note", "account-5", null))
        .isInstanceOf(NullPointerException.class);
    assertThat(monitor.getNotes()).isEmpty();
  }

  @Test
  void partialReplenishmentUpdatesQuantityAndRetainsOriginalShortageEvidence() {
    var monitor = monitor();
    var original = monitor.registerLowStock("medication-1", "Losartan", 0, 5, NOW);
    monitor.registerLowStock("medication-2", "Metformin", 1, 3, NOW);
    monitor.resolveLowStock("medication-1", 3);
    var notice = monitor.getLowStockNotices().getFirst();
    assertThat(original.getRemainingStock()).isZero();
    assertThat(notice.getRemainingStock()).isEqualTo(3);
    assertThat(notice.getDetectedAt()).isEqualTo(NOW);
    assertThat(notice.getMedicationName()).isEqualTo("Losartan");
    assertThat(notice.getReplenishmentThreshold()).isEqualTo(5);
    monitor.resolveLowStock("medication-1", 5);
    assertThat(monitor.getLowStockNotices().getFirst().getRemainingStock()).isEqualTo(5);
    assertThatThrownBy(() -> monitor.resolveLowStock("medication-1", -1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(monitor.getLowStockNotices().getFirst().getRemainingStock()).isEqualTo(5);
    monitor.resolveLowStock("missing", 10);
    assertThat(monitor.getLowStockNotices()).hasSize(2);
    monitor.resolveLowStock("medication-1", 6);
    assertThat(monitor.getLowStockNotices())
        .extracting(value -> value.getMedicationId())
        .containsExactly("medication-2");
    monitor.resolveLowStock("medication-1", 0);
    assertThat(monitor.getLowStockNotices())
        .extracting(value -> value.getMedicationId())
        .containsExactly("medication-2");
  }

  @Test
  void returnedAndRestoredChildReferencesCannotMutateOwnedHistory() {
    var alert =
        AlertSummary.rehydrate(
            1L, "intake-100", "Losartan", NOW, "Omitted", AlertStatus.OPEN, NOW, null);
    var monitor =
        FamilyMonitor.rehydrate(
            1L,
            "link",
            "adult",
            "caregiver",
            List.of(alert),
            List.of(),
            List.of(),
            List.of(),
            NOW,
            NOW,
            0L);
    alert.close(NOW);
    monitor.getAlerts().getFirst().close(NOW);
    monitor.findAlert(1L).markAttended();
    monitor.pendingAlerts().getFirst().close(NOW);
    assertThat(monitor.findAlert(1L).getStatus()).isEqualTo(AlertStatus.OPEN);
    var returned = monitor.registerLowStock("medication", "Losartan", 2, 5, NOW);
    returned.refresh("Changed", 0, 5, NOW);
    monitor.getLowStockNotices().getFirst().refresh("Changed", 1, 5, NOW);
    assertThat(monitor.getLowStockNotices().getFirst().getRemainingStock()).isEqualTo(2);
    assertThat(monitor.getLowStockNotices().getFirst().getMedicationName()).isEqualTo("Losartan");
  }
}
