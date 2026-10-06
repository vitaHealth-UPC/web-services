package com.tata.intakeexecution;

import com.tata.intakeexecution.application.commands.ReportUnconfirmedIntakesCommand;
import com.tata.intakeexecution.application.internal.commandservices.ReportUnconfirmedIntakesCommandHandler;
import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.tata.omissionescalation.application.internal.commandservices.EvaluateGracePeriodCommandHandler;
import com.tata.omissionescalation.domain.model.commands.EvaluateGracePeriodCommand;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AutomaticUnconfirmedIntakeIntegrationTest {

    @Autowired IntakeRepository intakes;
    @Autowired IOmissionCaseRepository omissionCases;
    @Autowired ReportUnconfirmedIntakesCommandHandler reportUnconfirmed;
    @Autowired EvaluateGracePeriodCommandHandler evaluateGracePeriod;

    @Test
    void unresolvedIntakeIsReportedOnceAndBecomesDefinitivelyOmitted() {
        var scheduledAt = Instant.now().minusSeconds(1800);
        var intake = Intake.createScheduled(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                new MedicationSnapshot("Losartan 50 mg", "1 tablet", "With water"),
                scheduledAt,
                scheduledAt.minusSeconds(60)
        );
        intake = intakes.saveAll(List.of(intake)).getFirst();

        var reportedAt = Instant.now();

        assertEquals(
                1,
                reportUnconfirmed.handle(new ReportUnconfirmedIntakesCommand(reportedAt, reportedAt))
        );
        assertEquals(
                0,
                reportUnconfirmed.handle(new ReportUnconfirmedIntakesCommand(reportedAt, reportedAt.plusSeconds(1)))
        );

        var reportedIntake = intakes.findById(intake.id()).orElseThrow();
        assertNotNull(reportedIntake.unconfirmedReportedAt());

        var omissionCase = omissionCases.findByIntakeId(intake.id()).orElseThrow();
        assertEquals(OmissionCaseStatus.PENDING, omissionCase.getStatus());
        assertNotNull(omissionCase.getReinforcedReminderSentAt());

        evaluateGracePeriod.handle(new EvaluateGracePeriodCommand(reportedAt.plusSeconds(5)));

        assertEquals(
                OmissionCaseStatus.OMITTED,
                omissionCases.findByIntakeId(intake.id()).orElseThrow().getStatus()
        );
        assertEquals(
                IntakeStatus.OMITTED,
                intakes.findById(intake.id()).orElseThrow().status()
        );
    }
}