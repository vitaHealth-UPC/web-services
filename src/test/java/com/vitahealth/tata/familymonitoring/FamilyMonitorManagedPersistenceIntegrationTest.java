package com.vitahealth.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories.CareLinkJpaRepository;
import com.vitahealth.tata.familymonitoring.application.internal.commandservices.AuthorizedFamilyMonitorProjectionService;
import com.vitahealth.tata.familymonitoring.application.internal.commandservices.InitializeFamilyMonitorCommandHandler;
import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.model.commands.InitializeFamilyMonitorCommand;
import com.vitahealth.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.vitahealth.tata.familymonitoring.domain.model.entities.CaregiverNote;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.InventoryStockPosition;
import com.vitahealth.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.repositories.FamilyMonitorJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Exercises committed managed writes, stale snapshots and owned history with an unchanged clock.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(FamilyMonitorManagedPersistenceIntegrationTest.FixedClockConfiguration.class)
class FamilyMonitorManagedPersistenceIntegrationTest {
  private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");
  @Autowired IFamilyMonitorRepository repository;
  @Autowired FamilyMonitorJpaRepository rows;
  @Autowired JdbcTemplate jdbc;
  @Autowired CareLinkRepository links;
  @Autowired CareLinkJpaRepository linkRows;
  @Autowired PlatformTransactionManager transactions;
  @Autowired AuthorizedFamilyMonitorProjectionService projections;
  @Autowired InitializeFamilyMonitorCommandHandler initialization;
  private final List<Long> ownedIds = new ArrayList<>();
  private final List<String> ownedLinks = new ArrayList<>();

  @TestConfiguration(proxyBeanMethods = false)
  static class FixedClockConfiguration {
    @Bean
    @Primary
    Clock familyPersistenceClock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }

  private FamilyMonitor stored() {
    return storedForAdult(UUID.randomUUID().toString());
  }

  private FamilyMonitor storedForAdult(String adultId) {
    var caregiver = UUID.randomUUID().toString();
    var code = UUID.randomUUID().toString().replace("-", "");
    var relationship =
        CareLink.createPending(
            caregiver, adultId, LinkingCode.issue(code, NOW, Duration.ofMinutes(15)), NOW);
    relationship.accept(code, NOW);
    relationship.registerConsent(true, NOW);
    relationship.confirm(NOW);
    links.save(relationship);
    ownedLinks.add(relationship.id());
    var source = new FamilyMonitor(relationship.id(), adultId, caregiver);
    source.addNote("Original intervention", source.getFamiliarId(), NOW);
    source.addAlert(
        UUID.randomUUID().toString(), "Losartan", NOW.minusSeconds(1800), "omission", NOW);
    var saved = repository.save(source);
    ownedIds.add(saved.getId());
    return saved;
  }

  @Test
  void healthyStockPositionSurvivesReloadAndRejectsOlderShortage() {
    var monitor = stored();
    var medication = UUID.randomUUID().toString();
    var inventory = UUID.randomUUID().toString();
    var low = new InventoryStockPosition(medication, inventory, 1, 2, 3, NOW);
    assertTrue(monitor.projectInventoryStock(low, "Losartan"));
    monitor = repository.save(monitor);
    assertTrue(monitor.hasLowStock());
    var healthy = new InventoryStockPosition(medication, inventory, 2, 10, 3, NOW);
    assertTrue(monitor.projectInventoryStock(healthy, "Losartan"));
    repository.save(monitor);
    var restored = repository.findById(monitor.getId()).orElseThrow();
    assertFalse(restored.hasLowStock());
    assertEquals(List.of(healthy), restored.getInventoryStockPositions());
    assertFalse(restored.projectInventoryStock(low, "Losartan"));
    assertFalse(restored.projectInventoryStock(healthy, "Losartan"));
    repository.save(restored);
    assertEquals(
        1,
        jdbc.queryForObject(
            "select count(*) from family_monitor_inventory_positions where family_monitor_id = ?",
            Integer.class,
            restored.getId()));
    assertFalse(repository.findById(restored.getId()).orElseThrow().hasLowStock());
  }

  @Test
  void repeatedLowStockPreservesOriginalDetectionAndRejectsConflictingRevision() {
    var monitor = stored();
    var medication = UUID.randomUUID().toString();
    var inventory = UUID.randomUUID().toString();
    monitor.projectInventoryStock(
        new InventoryStockPosition(medication, inventory, 1, 2, 3, NOW), "Losartan");
    monitor = repository.save(monitor);
    var noticeId = monitor.getLowStockNotices().getFirst().getId();
    monitor.projectInventoryStock(
        new InventoryStockPosition(medication, inventory, 2, 1, 3, NOW.plusSeconds(1)), "Losartan");
    monitor = repository.save(monitor);
    assertEquals(noticeId, monitor.getLowStockNotices().getFirst().getId());
    assertEquals(NOW, monitor.getLowStockNotices().getFirst().getDetectedAt());
    assertEquals(1, monitor.getLowStockNotices().getFirst().getRemainingStock());
    var restored = repository.findById(monitor.getId()).orElseThrow();
    assertThrows(
        IllegalStateException.class,
        () ->
            restored.projectInventoryStock(
                new InventoryStockPosition(medication, inventory, 2, 3, 3, NOW.plusSeconds(1)),
                "Losartan"));
    assertEquals(1, restored.getLowStockNotices().getFirst().getRemainingStock());
  }

  @AfterEach
  void removeOwnedRows() {
    for (Long id : ownedIds) rows.deleteById(id);
    for (String id : ownedLinks) linkRows.deleteById(id);
  }

  @Test
  void sourceEvidenceIsUniqueWithinOneFollowUpButMayReachAnotherCaregiver() {
    var first = stored();
    var second = stored();
    String[] inserts = {
      "insert into alert_summaries (family_monitor_id, intake_id, medication_name, scheduled_at,"
          + " reason, status, opened_at) values (?, 'shared-intake', 'Name', ?, 'omission', 'OPEN',"
          + " ?)",
      "insert into low_stock_notices (family_monitor_id, medication_id, medication_name,"
          + " remaining_stock, replenishment_threshold, detected_at) values (?,"
          + " 'shared-medication', 'Name', 1, 5, ?)",
      "insert into adherence_insights (family_monitor_id, medication_id, medication_name,"
          + " omission_days, first_day, last_day, detected_at) values (?, 'shared-medication',"
          + " 'Name', 1, DATE '2026-10-18', DATE '2026-10-18', ?)"
    };
    for (int index = 0; index < inserts.length; index++) {
      var sql = inserts[index];
      boolean alert = index == 0;
      for (var monitor : List.of(first, second))
        new TransactionTemplate(transactions)
            .executeWithoutResult(
                status -> {
                  if (alert)
                    jdbc.update(
                        sql,
                        monitor.getId(),
                        java.sql.Timestamp.from(NOW),
                        java.sql.Timestamp.from(NOW));
                  else jdbc.update(sql, monitor.getId(), java.sql.Timestamp.from(NOW));
                });
      assertThrows(
          DataIntegrityViolationException.class,
          () ->
              new TransactionTemplate(transactions)
                  .executeWithoutResult(
                      status -> {
                        if (alert)
                          jdbc.update(
                              sql,
                              first.getId(),
                              java.sql.Timestamp.from(NOW),
                              java.sql.Timestamp.from(NOW));
                        else jdbc.update(sql, first.getId(), java.sql.Timestamp.from(NOW));
                      }));
    }
    for (var monitor : List.of(first, second)) {
      var current = repository.findById(monitor.getId()).orElseThrow();
      assertEquals(2, current.getAlerts().size());
      assertEquals(1, current.getLowStockNotices().size());
      assertEquals(1, current.getAdherenceInsights().size());
      assertEquals(monitor.getRevision(), current.getRevision());
      current.addNote("Healthy write after rejected duplicate", current.getFamiliarId(), NOW);
      assertEquals(2, repository.save(current).getNotes().size());
    }
  }

  @Test
  void duplicateRelationshipCannotCreateASecondFollowUpOrReplaceOriginalEvidence() {
    var original = stored();
    var duplicate =
        new FamilyMonitor(
            original.getCareLinkId(), original.getOlderAdultId(), original.getFamiliarId());
    assertThrows(DataIntegrityViolationException.class, () -> repository.save(duplicate));
    var current = repository.findByCareLinkId(original.getCareLinkId()).orElseThrow();
    assertEquals(original.getId(), current.getId());
    assertEquals(original.getRevision(), current.getRevision());
    assertEquals(original.getNotes().getFirst().getId(), current.getNotes().getFirst().getId());
    assertEquals(
        1,
        jdbc.queryForObject(
            "select count(*) from family_monitors where care_link_id = ?",
            Integer.class,
            original.getCareLinkId()));
  }

  @Test
  void confirmationReplayPreservesHistoryButRejectsDifferentParticipantIdentities() {
    var original = stored();
    initialization.handle(
        new InitializeFamilyMonitorCommand(
            original.getCareLinkId(), original.getOlderAdultId(), original.getFamiliarId()));
    assertThrows(
        IllegalStateException.class,
        () ->
            initialization.handle(
                new InitializeFamilyMonitorCommand(
                    original.getCareLinkId(),
                    UUID.randomUUID().toString(),
                    original.getFamiliarId())));
    assertThrows(
        IllegalStateException.class,
        () ->
            initialization.handle(
                new InitializeFamilyMonitorCommand(
                    original.getCareLinkId(),
                    original.getOlderAdultId(),
                    UUID.randomUUID().toString())));
    var current = repository.findByCareLinkId(original.getCareLinkId()).orElseThrow();
    assertEquals(original.getId(), current.getId());
    assertEquals(original.getRevision(), current.getRevision());
    assertEquals(original.getNotes().getFirst().getId(), current.getNotes().getFirst().getId());
    assertEquals(original.getOlderAdultId(), current.getOlderAdultId());
    assertEquals(original.getFamiliarId(), current.getFamiliarId());
  }

  @Test
  void cachedSnapshotInAnotherTransactionCannotOverwriteTheCommittedWinner() throws Exception {
    var original = stored();
    var cached = new CountDownLatch(1);
    var winnerCommitted = new CountDownLatch(1);
    try (var executor = Executors.newSingleThreadExecutor()) {
      var staleAttempt =
          executor.submit(
              () ->
                  new TransactionTemplate(transactions)
                      .executeWithoutResult(
                          status -> {
                            var stale = repository.findById(original.getId()).orElseThrow();
                            cached.countDown();
                            await(winnerCommitted);
                            stale.addNote(
                                "Concurrent stale intervention", stale.getFamiliarId(), NOW);
                            repository.save(stale);
                          }));
      try {
        assertTrue(cached.await(20, TimeUnit.SECONDS));
        var winner = repository.findById(original.getId()).orElseThrow();
        winner.addNote("Concurrent committed intervention", winner.getFamiliarId(), NOW);
        repository.save(winner);
      } finally {
        winnerCommitted.countDown();
      }
      var failure =
          assertThrows(ExecutionException.class, () -> staleAttempt.get(20, TimeUnit.SECONDS));
      assertInstanceOf(OptimisticLockingFailureException.class, failure.getCause());
    }
    var current = repository.findById(original.getId()).orElseThrow();
    assertEquals(2, current.getNotes().size());
    assertTrue(
        current.getNotes().stream()
            .anyMatch(note -> note.getText().equals("Concurrent committed intervention")));
    assertTrue(
        current.getNotes().stream()
            .noneMatch(note -> note.getText().equals("Concurrent stale intervention")));
    assertEquals(original.getNotes().getFirst().getId(), current.getNotes().getFirst().getId());
  }

  @Test
  void failureAfterTheFirstProjectionFlushRollsBackEveryFollowUpAndAllowsRetry() {
    var adultId = UUID.randomUUID().toString();
    var first = storedForAdult(adultId);
    var second = storedForAdult(adultId);
    var calls = new AtomicInteger();
    assertThrows(
        IllegalStateException.class,
        () ->
            new TransactionTemplate(transactions)
                .executeWithoutResult(
                    status ->
                        projections.apply(
                            adultId,
                            monitor -> {
                              if (calls.incrementAndGet() == 2) {
                                assertEquals(
                                    2,
                                    jdbc.queryForObject(
                                        "select count(*) from caregiver_notes where"
                                            + " family_monitor_id = ?",
                                        Integer.class,
                                        first.getId()));
                                throw new IllegalStateException(
                                    "simulated source projection failure");
                              }
                              monitor.addNote(
                                  "Projected intervention", monitor.getFamiliarId(), NOW);
                            })));
    assertEquals(2, calls.get());
    for (var source : List.of(first, second)) {
      var current = repository.findById(source.getId()).orElseThrow();
      assertEquals(source.getRevision(), current.getRevision());
      assertEquals(1, current.getNotes().size());
      assertEquals(source.getNotes().getFirst().getId(), current.getNotes().getFirst().getId());
    }
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status ->
                projections.apply(
                    adultId,
                    monitor ->
                        monitor.addNote("Retry intervention", monitor.getFamiliarId(), NOW)));
    for (var source : List.of(first, second))
      assertEquals(2, repository.findById(source.getId()).orElseThrow().getNotes().size());
  }

  private static void await(CountDownLatch latch) {
    try {
      assertTrue(latch.await(20, TimeUnit.SECONDS));
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new AssertionError("interrupted while awaiting committed winner", interrupted);
    }
  }

  @Test
  void staleSnapshotCannotOverwriteCommittedNotesOrAlertHistory() {
    var original = stored();
    var first = repository.findById(original.getId()).orElseThrow();
    var stale = repository.findById(original.getId()).orElseThrow();
    first.addNote("Committed intervention", first.getFamiliarId(), NOW);
    var saved = repository.save(first);
    stale.addNote("Stale intervention", stale.getFamiliarId(), NOW);

    assertThrows(OptimisticLockingFailureException.class, () -> repository.save(stale));

    var current = repository.findById(original.getId()).orElseThrow();
    assertEquals(2, current.getNotes().size());
    assertTrue(
        current.getNotes().stream().noneMatch(note -> note.getText().equals("Stale intervention")));
    assertEquals(original.getNotes().getFirst().getId(), current.getNotes().getFirst().getId());
    assertEquals(original.getAlerts().getFirst().getId(), current.getAlerts().getFirst().getId());
    assertEquals(original.getCreatedAt(), current.getCreatedAt());
    assertEquals(saved.getRevision(), current.getRevision());
  }

  @Test
  void childOnlyChangeAdvancesRevisionEvenWhenAuditClockDoesNotAdvance() {
    var original = stored();
    original.markAlertAttended(original.getAlerts().getFirst().getId());
    var saved = repository.save(original);

    assertTrue(saved.getRevision() > original.getRevision());
    assertEquals(original.getCreatedAt(), saved.getCreatedAt());
    assertEquals(original.getUpdatedAt(), saved.getUpdatedAt());
    assertEquals(
        saved.getRevision(),
        jdbc.queryForObject(
            "select revision from family_monitors where id = ?", Long.class, saved.getId()));
    assertEquals(original.getAlerts().getFirst().getId(), saved.getAlerts().getFirst().getId());
  }

  @Test
  void rejectsForeignChildrenAndRemovedHistoryBeforeChangingStoredEvidence() {
    var source = stored();
    var other = stored();
    var before = jdbc.queryForMap("select * from family_monitors where id = ?", source.getId());
    var notes = new ArrayList<>(source.getNotes());
    notes.add(
        CaregiverNote.rehydrate(
            other.getNotes().getFirst().getId(),
            "Foreign stored identity",
            NOW,
            source.getFamiliarId()));
    var foreign = copy(source, notes, source.getAlerts());
    assertThrows(RuntimeException.class, () -> repository.save(foreign));
    var removed = copy(source, source.getNotes(), List.of());
    assertThrows(RuntimeException.class, () -> repository.save(removed));

    var current = repository.findById(source.getId()).orElseThrow();
    assertEquals(
        before, jdbc.queryForMap("select * from family_monitors where id = ?", source.getId()));
    assertEquals(source.getNotes().getFirst().getId(), current.getNotes().getFirst().getId());
    assertEquals(source.getAlerts().getFirst().getId(), current.getAlerts().getFirst().getId());
    current.addNote("Healthy next intervention", current.getFamiliarId(), NOW);
    assertEquals(2, repository.save(current).getNotes().size());
  }

  private FamilyMonitor copy(
      FamilyMonitor source, List<CaregiverNote> notes, List<AlertSummary> alerts) {
    return FamilyMonitor.rehydrate(
        source.getId(),
        source.getCareLinkId(),
        source.getOlderAdultId(),
        source.getFamiliarId(),
        alerts,
        notes,
        source.getLowStockNotices(),
        source.getAdherenceInsights(),
        source.getCreatedAt(),
        source.getUpdatedAt(),
        source.getRevision());
  }
}
