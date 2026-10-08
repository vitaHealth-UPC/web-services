package com.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tata.adherenceanalytics.application.internal.commandservices.ConsolidateWeeklyPeriodCommandHandler;
import com.tata.adherenceanalytics.domain.model.events.AdherencePatternDetected;
import com.tata.adherenceanalytics.infrastructure.persistence.jpa.repositories.AdherenceSnapshotJpaRepository;
import com.tata.adherenceanalytics.infrastructure.scheduling.WeeklyConsolidationScheduler;
import com.tata.familymonitoring.application.internal.queryservices.GetOlderAdultStatusQueryHandler;
import com.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.tata.familymonitoring.domain.model.queries.GetOlderAdultStatusQuery;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.tata.inventoryreplenishment.domain.model.events.LowStockDetected;
import com.tata.inventoryreplenishment.domain.model.events.ReplenishmentRegistered;
import com.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Events from Inventory and Adherence Analytics reach the caregiver's follow-up. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CrossContextConnectionsTest {

    @Autowired ApplicationEventPublisher events;
    @Autowired IFamilyMonitorRepository monitors;
    @Autowired MedicationRepository medications;
    @Autowired GetOlderAdultStatusQueryHandler status;
    @Autowired ConsolidateWeeklyPeriodCommandHandler consolidations;
    @Autowired WeeklyConsolidationScheduler scheduler;
    @Autowired AdherenceSnapshotJpaRepository snapshots;
    @Autowired IntakeRepository intakes;

    private String monitoredOlderAdult() {
        var olderAdultId = UUID.randomUUID().toString();
        monitors.save(new FamilyMonitor(UUID.randomUUID().toString(), olderAdultId, UUID.randomUUID().toString()));
        return olderAdultId;
    }

    private Medication medicationOf(String olderAdultId, String name) {
        return medications.save(Medication.register(olderAdultId, name, "50 mg", Instant.now()));
    }

    private void lowStock(String medicationId, int remaining, int threshold) {
        events.publishEvent(new LowStockDetected(UUID.randomUUID().toString(), medicationId, remaining, threshold, Instant.now()));
    }

    private void replenished(String medicationId, int remaining) {
        events.publishEvent(new ReplenishmentRegistered(
                UUID.randomUUID().toString(), medicationId, UUID.randomUUID().toString(), 30, remaining, Instant.now()));
    }

    @Test
    void lowStockAppearsInTheStatusOfTheOlderAdultWhoOwnsTheMedication() {
        var olderAdultId = monitoredOlderAdult();
        var medication = medicationOf(olderAdultId, "Losartán");

        lowStock(medication.id(), 3, 5);

        var notices = status.handle(new GetOlderAdultStatusQuery(olderAdultId)).lowStockNotices();
        assertEquals(1, notices.size());
        assertEquals("Losartán", notices.getFirst().getMedicationName());
        assertEquals(3, notices.getFirst().getRemainingStock());
    }

    @Test
    void aSecondLowStockForTheSameMedicationOnlyRefreshesTheNotice() {
        var olderAdultId = monitoredOlderAdult();
        var medication = medicationOf(olderAdultId, "Losartán");

        lowStock(medication.id(), 4, 5);
        lowStock(medication.id(), 2, 5);

        var notices = status.handle(new GetOlderAdultStatusQuery(olderAdultId)).lowStockNotices();
        assertEquals(1, notices.size());
        assertEquals(2, notices.getFirst().getRemainingStock());
    }

    @Test
    void aReplenishmentClearsTheNoticeOnlyWhenTheStockIsAboveTheThreshold() {
        var olderAdultId = monitoredOlderAdult();
        var medication = medicationOf(olderAdultId, "Losartán");
        lowStock(medication.id(), 2, 5);

        replenished(medication.id(), 5);
        assertEquals(1, status.handle(new GetOlderAdultStatusQuery(olderAdultId)).lowStockNotices().size());

        replenished(medication.id(), 35);
        assertTrue(status.handle(new GetOlderAdultStatusQuery(olderAdultId)).lowStockNotices().isEmpty());
    }

    @Test
    void lowStockOfAnUnknownMedicationOrWithoutMonitorIsIgnored() {
        lowStock(UUID.randomUUID().toString(), 1, 5);
        var withoutMonitor = medicationOf(UUID.randomUUID().toString(), "Metformina");

        lowStock(withoutMonitor.id(), 1, 5);

        assertTrue(monitors.findByOlderAdultId(withoutMonitor.olderAdultId()).isEmpty());
    }

    @Test
    void anAdherencePatternBecomesAnInsightOnlyOnce() {
        var olderAdultId = monitoredOlderAdult();
        var medication = medicationOf(olderAdultId, "Losartán");
        var detected = new AdherencePatternDetected(
                olderAdultId, medication.id(), 3, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-03"), Instant.now());

        events.publishEvent(detected);
        events.publishEvent(detected);

        var insights = status.handle(new GetOlderAdultStatusQuery(olderAdultId)).insights();
        assertEquals(1, insights.size());
        assertEquals("Losartán", insights.getFirst().getMedicationName());
        assertEquals(3, insights.getFirst().getOmissionDays());
    }

    private void omittedDays(String olderAdultId, String medicationId, Instant firstDayStart, int days) {
        for (int day = 0; day < days; day++) {
            var intake = Intake.createScheduled(UUID.randomUUID().toString(), medicationId, olderAdultId,
                    new MedicationSnapshot("Losartán", "1 tablet", ""), firstDayStart.plusSeconds(day * 86400L), firstDayStart);
            intake.markOmitted();
            intakes.saveAll(List.of(intake));
        }
    }

    @Test
    void aConsolidationWithRepeatedOmissionsReachesTheCaregiver() {
        var olderAdultId = monitoredOlderAdult();
        var medication = medicationOf(olderAdultId, "Losartán");
        var from = Instant.parse("2026-10-01T00:00:00Z");
        omittedDays(olderAdultId, medication.id(), from, 3);

        consolidations.handle(olderAdultId, from, from.plusSeconds(7 * 86400L), "UTC");
        consolidations.handle(olderAdultId, from, from.plusSeconds(7 * 86400L), "UTC");

        var insights = status.handle(new GetOlderAdultStatusQuery(olderAdultId)).insights();
        assertEquals(1, insights.size());
        assertEquals("Losartán", insights.getFirst().getMedicationName());
    }

    @Test
    void theWeeklySchedulerConsolidatesTheLastSevenDaysOnceAndNotifiesTheCaregiver() {
        var olderAdultId = monitoredOlderAdult();
        var medication = medicationOf(olderAdultId, "Losartán");
        // Lima is UTC-5: the week ends at 2026-10-08T05:00:00Z and starts at 2026-10-01T05:00:00Z
        omittedDays(olderAdultId, medication.id(), Instant.parse("2026-10-01T06:00:00Z"), 3);
        var now = Instant.parse("2026-10-08T15:00:00Z");

        scheduler.consolidateWeekEnding(now);
        scheduler.consolidateWeekEnding(now.plusSeconds(3600));

        var stored = snapshots.findAll().stream()
                .filter(snapshot -> snapshot.olderAdultId().equals(olderAdultId))
                .toList();
        assertEquals(1, stored.size());
        assertEquals(3, stored.getFirst().omittedIntakes());
        assertEquals(1, status.handle(new GetOlderAdultStatusQuery(olderAdultId)).insights().size());
    }
}
