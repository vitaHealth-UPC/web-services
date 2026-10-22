package com.vitahealth.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;

import com.vitahealth.tata.adherenceanalytics.application.internal.commandservices.ConsolidateWeeklyPeriodCommandHandler;
import com.vitahealth.tata.adherenceanalytics.domain.model.commands.ConsolidateWeeklyPeriodCommand;
import com.vitahealth.tata.adherenceanalytics.domain.model.events.AdherencePatternDetected;
import com.vitahealth.tata.adherenceanalytics.infrastructure.events.SpringAdherenceEventPublisher;
import com.vitahealth.tata.adherenceanalytics.infrastructure.persistence.jpa.repositories.AdherenceSnapshotJpaRepository;
import com.vitahealth.tata.adherenceanalytics.infrastructure.scheduling.WeeklyConsolidationScheduler;
import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.familymonitoring.application.internal.queryservices.GetOlderAdultStatusQueryHandler;
import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetOlderAdultStatusQuery;
import com.vitahealth.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.inventoryreplenishment.application.commandservices.InventoryCommandService;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.ConsumeUnitCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RegisterInitialInventoryCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RegisterReplenishmentCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.events.InventoryStockChanged;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Events from Inventory and Adherence Analytics reach the caregiver's follow-up. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CrossContextConnectionsTest {

  @Autowired ApplicationEventPublisher events;
  @Autowired InventoryCommandService inventories;
  @Autowired IFamilyMonitorRepository monitors;
  @Autowired MedicationRepository medications;
  @Autowired GetOlderAdultStatusQueryHandler status;
  @Autowired ConsolidateWeeklyPeriodCommandHandler consolidations;
  @Autowired WeeklyConsolidationScheduler scheduler;
  @Autowired AdherenceSnapshotJpaRepository snapshots;
  @Autowired IntakeRepository intakes;
  @Autowired CareLinkRepository links;
  @Autowired Clock clock;
  @Autowired JdbcTemplate jdbc;
  @MockitoSpyBean SpringAdherenceEventPublisher analyticsPublisher;

  private final Map<String, String> caregivers = new HashMap<>();

  @Test
  void invalidInventoryEvidenceCannotChangeExistingFollowUpOrBlockHealthyResolution() {
    var adult = monitoredOlderAdult();
    var medication = medicationOf(adult, "Losartan");
    lowStock(medication.id(), 3, 5);
    var monitorId = monitors.findIdsByOlderAdultIdAfter(adult, 0, 1).getFirst();
    var parentBefore = jdbc.queryForMap("select * from family_monitors where id = ?", monitorId);
    var stockBefore =
        jdbc.queryForList("select * from low_stock_notices where family_monitor_id = ?", monitorId);
    assertEquals(1, stockBefore.size());
    var now = clock.instant();
    for (int[] invalid : new int[][] {{-1, 5}, {0, -1}})
      assertThrows(
          IllegalArgumentException.class,
          () ->
              events.publishEvent(
                  new InventoryStockChanged(
                      UUID.randomUUID().toString(),
                      medication.id(),
                      invalid[0],
                      invalid[1],
                      1,
                      now)));
    assertThrows(
        NullPointerException.class,
        () ->
            events.publishEvent(
                new InventoryStockChanged(
                    UUID.randomUUID().toString(), medication.id(), 1, 5, 1, null)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            events.publishEvent(
                new InventoryStockChanged(UUID.randomUUID().toString(), " ", 1, 5, 1, now)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            events.publishEvent(
                new InventoryStockChanged(
                    UUID.randomUUID().toString(), medication.id(), 1, 5, -1, now)));
    assertEquals(
        parentBefore, jdbc.queryForMap("select * from family_monitors where id = ?", monitorId));
    assertEquals(
        stockBefore,
        jdbc.queryForList(
            "select * from low_stock_notices where family_monitor_id = ?", monitorId));
    replenished(medication.id(), 6);
    assertTrue(
        jdbc.queryForList("select * from low_stock_notices where family_monitor_id = ?", monitorId)
            .isEmpty());
  }

  private String monitoredOlderAdult() {
    var olderAdultId = UUID.randomUUID().toString();
    var caregiverId = UUID.randomUUID().toString();
    caregivers.put(olderAdultId, caregiverId);
    var now = clock.instant();
    var code = UUID.randomUUID().toString().replace("-", "");
    var link =
        CareLink.createPending(
            caregiverId, olderAdultId, LinkingCode.issue(code, now, Duration.ofMinutes(15)), now);
    link.accept(code, now);
    link.registerConsent(true, now);
    link.confirm(now);
    links.save(link);
    monitors.save(new FamilyMonitor(link.id(), olderAdultId, caregiverId));
    return olderAdultId;
  }

  private Medication medicationOf(String olderAdultId, String name) {
    return medications.save(Medication.register(olderAdultId, name, "50 mg", Instant.now()));
  }

  private final Map<String, String> inventoryIds = new HashMap<>();
  private final Map<String, Long> inventoryRevisions = new HashMap<>();

  private void lowStock(String medicationId, int remaining, int threshold) {
    publishStock(medicationId, remaining, threshold);
  }

  private void replenished(String medicationId, int remaining) {
    publishStock(medicationId, remaining, 5);
  }

  private void publishStock(String medicationId, int remaining, int threshold) {
    var inventoryId =
        inventoryIds.computeIfAbsent(medicationId, ignored -> UUID.randomUUID().toString());
    var revision = inventoryRevisions.merge(medicationId, 1L, Long::sum);
    events.publishEvent(
        new InventoryStockChanged(
            inventoryId, medicationId, remaining, threshold, revision, clock.instant()));
  }

  @Test
  void actualInventoryCommandsProjectInitialLowStockEveryConsumptionAndReplenishment() {
    var adult = monitoredOlderAdult();
    var medication = medicationOf(adult, "Losartan");
    inventories.registerInitialInventory(
        new RegisterInitialInventoryCommand(medication.id(), 3, 5));
    var monitor =
        monitors
            .findById(monitors.findIdsByOlderAdultIdAfter(adult, 0, 1).getFirst())
            .orElseThrow();
    assertEquals(3, monitor.getLowStockNotices().getFirst().getRemainingStock());
    var originalDetection = monitor.getLowStockNotices().getFirst().getDetectedAt();
    inventories.consumeUnit(new ConsumeUnitCommand(medication.id(), UUID.randomUUID().toString()));
    assertEquals(
        2,
        monitors
            .findById(monitor.getId())
            .orElseThrow()
            .getLowStockNotices()
            .getFirst()
            .getRemainingStock());
    inventories.registerReplenishment(new RegisterReplenishmentCommand(medication.id(), 1));
    var partial = monitors.findById(monitor.getId()).orElseThrow();
    assertEquals(3, partial.getLowStockNotices().getFirst().getRemainingStock());
    assertEquals(originalDetection, partial.getLowStockNotices().getFirst().getDetectedAt());
    inventories.registerReplenishment(new RegisterReplenishmentCommand(medication.id(), 10));
    var healthy = monitors.findById(monitor.getId()).orElseThrow();
    assertTrue(healthy.getLowStockNotices().isEmpty());
    var position = healthy.getInventoryStockPositions().getFirst();
    var revision = healthy.getRevision();
    events.publishEvent(
        new InventoryStockChanged(
            position.inventoryId(), medication.id(), 3, 5, 0, position.changedAt()));
    events.publishEvent(
        new InventoryStockChanged(
            position.inventoryId(),
            medication.id(),
            position.remainingStock(),
            5,
            position.revision(),
            position.changedAt()));
    var unchanged = monitors.findById(monitor.getId()).orElseThrow();
    assertTrue(unchanged.getLowStockNotices().isEmpty());
    assertEquals(revision, unchanged.getRevision());
  }

  @Test
  void lowStockAppearsInTheStatusOfTheOlderAdultWhoOwnsTheMedication() {
    var olderAdultId = monitoredOlderAdult();
    var medication = medicationOf(olderAdultId, "Losartán");

    lowStock(medication.id(), 3, 5);

    var notices =
        status
            .handle(new GetOlderAdultStatusQuery(olderAdultId, caregivers.get(olderAdultId)))
            .lowStockNotices();
    assertEquals(1, notices.size());
    assertEquals("Losartán", notices.getFirst().medicationName());
    assertEquals(3, notices.getFirst().remainingStock());
  }

  @Test
  void aSecondLowStockForTheSameMedicationOnlyRefreshesTheNotice() {
    var olderAdultId = monitoredOlderAdult();
    var medication = medicationOf(olderAdultId, "Losartán");

    lowStock(medication.id(), 4, 5);
    lowStock(medication.id(), 2, 5);

    var notices =
        status
            .handle(new GetOlderAdultStatusQuery(olderAdultId, caregivers.get(olderAdultId)))
            .lowStockNotices();
    assertEquals(1, notices.size());
    assertEquals(2, notices.getFirst().remainingStock());
  }

  @Test
  void aReplenishmentClearsTheNoticeOnlyWhenTheStockIsAboveTheThreshold() {
    var olderAdultId = monitoredOlderAdult();
    var medication = medicationOf(olderAdultId, "Losartán");
    lowStock(medication.id(), 2, 5);

    var monitorId = monitors.findIdsByOlderAdultIdAfter(olderAdultId, 0, 1).getFirst();
    var original =
        jdbc.queryForMap(
            "select id, detected_at from low_stock_notices where family_monitor_id = ?", monitorId);
    replenished(medication.id(), 4);
    assertEquals(
        4,
        status
            .handle(new GetOlderAdultStatusQuery(olderAdultId, caregivers.get(olderAdultId)))
            .lowStockNotices()
            .getFirst()
            .remainingStock());
    replenished(medication.id(), 5);
    var notices =
        status
            .handle(new GetOlderAdultStatusQuery(olderAdultId, caregivers.get(olderAdultId)))
            .lowStockNotices();
    assertEquals(1, notices.size());
    assertEquals(5, notices.getFirst().remainingStock());
    assertEquals(
        original,
        jdbc.queryForMap(
            "select id, detected_at from low_stock_notices where family_monitor_id = ?",
            monitorId));
    assertEquals(
        5,
        jdbc.queryForObject(
            "select remaining_stock from low_stock_notices where family_monitor_id = ?",
            Integer.class,
            monitorId));

    replenished(medication.id(), 35);
    assertTrue(
        status
            .handle(new GetOlderAdultStatusQuery(olderAdultId, caregivers.get(olderAdultId)))
            .lowStockNotices()
            .isEmpty());
  }

  @Test
  void lowStockOfAnUnknownMedicationOrWithoutMonitorIsIgnored() {
    lowStock(UUID.randomUUID().toString(), 1, 5);
    var withoutMonitor = medicationOf(UUID.randomUUID().toString(), "Metformina");

    lowStock(withoutMonitor.id(), 1, 5);

    assertTrue(monitors.findIdsByOlderAdultIdAfter(withoutMonitor.olderAdultId(), 0, 1).isEmpty());
  }

  @Test
  void anAdherencePatternBecomesAnInsightOnlyOnce() {
    var olderAdultId = monitoredOlderAdult();
    var medication = medicationOf(olderAdultId, "Losartán");
    var detected =
        new AdherencePatternDetected(
            olderAdultId,
            medication.id(),
            3,
            LocalDate.parse("2026-10-01"),
            LocalDate.parse("2026-10-03"),
            Instant.now());

    events.publishEvent(detected);
    events.publishEvent(detected);

    var insights =
        status
            .handle(new GetOlderAdultStatusQuery(olderAdultId, caregivers.get(olderAdultId)))
            .insights();
    assertEquals(1, insights.size());
    assertEquals("Losartán", insights.getFirst().medicationName());
    assertEquals(3, insights.getFirst().omissionDays());
  }

  private void omittedDays(
      String olderAdultId, String medicationId, Instant firstDayStart, int days) {
    for (int day = 0; day < days; day++) {
      var intake =
          Intake.createScheduled(
              UUID.randomUUID().toString(),
              medicationId,
              olderAdultId,
              new MedicationSnapshot("Losartán", "1 tablet", ""),
              firstDayStart.plusSeconds(day * 86400L),
              firstDayStart);
      intake.markOmitted();
      intakes.saveAll(List.of(intake));
    }
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void laterPatternFailureRollsBackAllInsightsAndSnapshotBeforeHealthyRetry() {
    var adult = monitoredOlderAdult();
    var first = medicationOf(adult, "Losartan");
    var second = medicationOf(adult, "Metformina");
    var from = Instant.parse("2026-10-01T00:00:00Z");
    var to = from.plusSeconds(7 * 86400L);
    omittedDays(adult, first.id(), from, 3);
    omittedDays(adult, second.id(), from, 3);
    var delivered = new AtomicInteger();
    doAnswer(
            invocation -> {
              invocation.callRealMethod();
              if (delivered.incrementAndGet() == 2) {
                assertEquals(
                    2,
                    status
                        .handle(new GetOlderAdultStatusQuery(adult, caregivers.get(adult)))
                        .insights()
                        .size());
                throw new IllegalStateException("controlled second-pattern failure");
              }
              return null;
            })
        .when(analyticsPublisher)
        .publish(argThat(event -> event.olderAdultId().equals(adult)));

    assertThrows(
        IllegalStateException.class,
        () -> consolidations.handle(new ConsolidateWeeklyPeriodCommand(adult, from, to, "UTC")));
    assertEquals(2, delivered.get());
    assertTrue(
        snapshots.findAll().stream().noneMatch(snapshot -> snapshot.olderAdultId().equals(adult)));
    assertTrue(
        status
            .handle(new GetOlderAdultStatusQuery(adult, caregivers.get(adult)))
            .insights()
            .isEmpty());

    doCallRealMethod()
        .when(analyticsPublisher)
        .publish(argThat(event -> event.olderAdultId().equals(adult)));
    var committed =
        consolidations.handle(new ConsolidateWeeklyPeriodCommand(adult, from, to, "UTC"));
    assertEquals(2, committed.patterns().size());
    assertEquals(
        2,
        status
            .handle(new GetOlderAdultStatusQuery(adult, caregivers.get(adult)))
            .insights()
            .size());
    consolidations.handle(new ConsolidateWeeklyPeriodCommand(adult, from, to, "UTC"));
    assertEquals(
        2,
        status
            .handle(new GetOlderAdultStatusQuery(adult, caregivers.get(adult)))
            .insights()
            .size());
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void aConsolidationWithRepeatedOmissionsReachesTheCaregiver() {
    var olderAdultId = monitoredOlderAdult();
    var medication = medicationOf(olderAdultId, "Losartán");
    var from = Instant.parse("2026-10-01T00:00:00Z");
    omittedDays(olderAdultId, medication.id(), from, 3);

    consolidations.handle(
        new ConsolidateWeeklyPeriodCommand(
            olderAdultId, from, from.plusSeconds(7 * 86400L), "UTC"));
    consolidations.handle(
        new ConsolidateWeeklyPeriodCommand(
            olderAdultId, from, from.plusSeconds(7 * 86400L), "UTC"));

    var insights =
        status
            .handle(new GetOlderAdultStatusQuery(olderAdultId, caregivers.get(olderAdultId)))
            .insights();
    assertEquals(1, insights.size());
    assertEquals("Losartán", insights.getFirst().medicationName());
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void theWeeklySchedulerConsolidatesTheLastSevenDaysOnceAndNotifiesTheCaregiver() {
    var olderAdultId = monitoredOlderAdult();
    var medication = medicationOf(olderAdultId, "Losartán");
    // Lima is UTC-5: the week ends at 2026-10-08T05:00:00Z and starts at 2026-10-01T05:00:00Z
    omittedDays(olderAdultId, medication.id(), Instant.parse("2026-10-01T06:00:00Z"), 3);
    var now = Instant.parse("2026-10-08T15:00:00Z");

    scheduler.consolidateWeekEnding(now);
    scheduler.consolidateWeekEnding(now.plusSeconds(3600));

    var stored =
        snapshots.findAll().stream()
            .filter(snapshot -> snapshot.olderAdultId().equals(olderAdultId))
            .toList();
    assertEquals(1, stored.size());
    assertEquals(3, stored.getFirst().omittedIntakes());
    assertEquals(
        1,
        status
            .handle(new GetOlderAdultStatusQuery(olderAdultId, caregivers.get(olderAdultId)))
            .insights()
            .size());
  }
}
