package com.vitahealth.tata.omissionescalation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.IDomainEventPublisher;
import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationIntentRecoveryScheduler;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationIntentJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.OmissionCaseJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Verifies old source recovery uses real SQL pages and preserves existing clinical evidence. */
@SpringBootTest
@ActiveProfiles("test")
class NotificationIntentRecoveryIntegrationTest {
  @Autowired NotificationIntentRecoveryScheduler recovery;
  @Autowired IOmissionCaseRepository cases;
  @Autowired OmissionCaseJpaRepository rows;
  @Autowired NotificationIntentJpaRepository intents;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;
  @MockitoBean IDomainEventPublisher events;
  private final List<Long> ids = new ArrayList<>();

  @AfterEach
  void cleanup() {
    for (long id : ids) {
      for (var kind : NotificationKind.values())
        intents.findBySourceCaseIdAndKind(id, kind).ifPresent(intents::delete);
      rows.deleteById(id);
    }
  }

  private OmissionCase pending() {
    var now = clock.instant().minusSeconds(120);
    var value =
        cases.save(
            new OmissionCase(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "Medication",
                now,
                GracePeriod.startingAt(now, Duration.ofMinutes(30))));
    ids.add(value.getId());
    return value;
  }

  @Test
  void corruptFirstCaseCannotStarve105CaseScanAndNoExistingIntentIsDuplicated() {
    var corrupt = pending();
    var healthy = new ArrayList<Long>();
    for (int i = 0; i < 104; i++) healthy.add(pending().getId());
    jdbc.update(
        "update omission_cases set closed_at = ? where id = ?",
        java.sql.Timestamp.from(clock.instant()),
        corrupt.getId());
    var original = jdbc.queryForMap("select * from omission_cases where id = ?", corrupt.getId());
    recovery.recover();
    assertThat(
            intents.findBySourceCaseIdAndKind(
                corrupt.getId(), NotificationKind.REINFORCED_REMINDER))
        .isEmpty();
    var stable =
        intents
            .findBySourceCaseIdAndKind(healthy.getLast(), NotificationKind.REINFORCED_REMINDER)
            .orElseThrow()
            .getId();
    for (long id : healthy)
      assertThat(intents.findBySourceCaseIdAndKind(id, NotificationKind.REINFORCED_REMINDER))
          .isPresent();
    recovery.recover();
    assertThat(
            intents
                .findBySourceCaseIdAndKind(healthy.getLast(), NotificationKind.REINFORCED_REMINDER)
                .orElseThrow()
                .getId())
        .isEqualTo(stable);
    assertThat(jdbc.queryForMap("select * from omission_cases where id = ?", corrupt.getId()))
        .isEqualTo(original);
  }

  @Test
  void historicalFailedAlertRetainsIdentityDatesAndFailureWhenIntentIsRecovered() {
    var now = clock.instant();
    var source =
        new OmissionCase(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Medication",
            now.minusSeconds(3600),
            GracePeriod.startingAt(now.minusSeconds(3600), Duration.ZERO));
    source.markOmitted(now.minusSeconds(300));
    source.addAlert(now.minusSeconds(200));
    source.markAlertFailed("historical reason");
    var saved = cases.save(source);
    ids.add(saved.getId());
    var before = saved.getAlerts().getFirst();
    recovery.recover();
    var intent =
        intents
            .findBySourceCaseIdAndKind(saved.getId(), NotificationKind.CAREGIVER_ALERT)
            .orElseThrow();
    recovery.recover();
    var after = cases.findById(saved.getId()).orElseThrow();
    assertThat(after.getAlerts().getFirst().getId()).isEqualTo(before.getId());
    assertThat(after.getAlerts().getFirst().getGeneratedAt()).isEqualTo(before.getGeneratedAt());
    assertThat(after.getAlerts().getFirst().getStatus()).isEqualTo(AlertStatus.FAILED);
    assertThat(after.getAlerts().getFirst().getFailureReason()).isEqualTo("historical reason");
    assertThat(after.getOmittedAt()).isEqualTo(saved.getOmittedAt());
    assertThat(
            intents
                .findBySourceCaseIdAndKind(saved.getId(), NotificationKind.CAREGIVER_ALERT)
                .orElseThrow()
                .getId())
        .isEqualTo(intent.getId());
    verifyNoInteractions(events);
  }
}
