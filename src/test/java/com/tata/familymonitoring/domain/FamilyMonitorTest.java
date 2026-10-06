package com.tata.familymonitoring.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tata.familymonitoring.domain.exceptions.AlertNotFoundException;
import com.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.tata.familymonitoring.domain.model.valueobjects.AlertStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FamilyMonitorTest {

  private static final Instant NOW = Instant.parse("2026-10-05T14:00:00Z");

  private FamilyMonitor monitor() {
    return new FamilyMonitor("link-1", "adult-10", "account-5");
  }

  private AlertSummary addAlert(FamilyMonitor monitor, String intakeId) {
    return monitor.addAlert(intakeId, "Losartan 50 mg", NOW.minusSeconds(1800), "Not confirmed", NOW);
  }

  @Test
  void addAlert_startsOpen() {
    FamilyMonitor monitor = monitor();

    addAlert(monitor, "intake-100");

    assertThat(monitor.hasOpenAlert()).isTrue();
    assertThat(monitor.openAlerts()).hasSize(1);
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
    FamilyMonitor monitor = monitor();
    AlertSummary alert = addAlert(monitor, "intake-100");

    alert.markAttended();
    alert.close(NOW.plusSeconds(60));

    assertThat(alert.getStatus()).isEqualTo(AlertStatus.CLOSED);
    assertThat(alert.getClosedAt()).isEqualTo(NOW.plusSeconds(60));
    assertThat(monitor.hasOpenAlert()).isFalse();
    assertThat(monitor.getAlerts()).hasSize(1);
  }

  @Test
  void close_aClosedAlert_isRejected() {
    AlertSummary alert = addAlert(monitor(), "intake-100");
    alert.close(NOW);

    assertThatThrownBy(() -> alert.close(NOW)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void markAttended_aClosedAlert_isRejected() {
    AlertSummary alert = addAlert(monitor(), "intake-100");
    alert.close(NOW);

    assertThatThrownBy(alert::markAttended).isInstanceOf(IllegalStateException.class);
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
}
