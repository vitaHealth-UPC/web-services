package com.tata.intakeexecution;

import com.tata.intakeexecution.application.commands.ConfirmIntakeCommand;
import com.tata.intakeexecution.application.internal.IntakeApplicationException;
import com.tata.intakeexecution.application.internal.commandservices.ConfirmIntakeCommandHandler;
import com.tata.intakeexecution.application.internal.commandservices.ReportUnconfirmedIntakeCommandHandler;
import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.valueobjects.*;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.tata.intakeexecution.infrastructure.scheduling.UnconfirmedIntakeScheduler;
import com.tata.omissionescalation.application.internal.commandservices.EvaluateGracePeriodCommandHandler;
import com.tata.omissionescalation.domain.model.commands.EvaluateGracePeriodCommand;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:automatic-omission;DB_CLOSE_DELAY=-1", "tata.omission.grace-period=PT30M", "tata.omission.evaluation-initial-delay=PT1H", "tata.intake.unconfirmed-initial-delay=PT1H"})
@ActiveProfiles("test")
@Transactional
class AutomaticOmissionIntegrationTest {
    @Autowired IntakeRepository intakes;
    @Autowired IOmissionCaseRepository cases;
    @Autowired UnconfirmedIntakeScheduler scheduler;
    @Autowired ReportUnconfirmedIntakeCommandHandler report;
    @Autowired ConfirmIntakeCommandHandler confirm;
    @Autowired EvaluateGracePeriodCommandHandler evaluate;
    private Intake intake(Instant scheduledAt) {
        return intakes.saveAll(List.of(Intake.createScheduled(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                new MedicationSnapshot("Losartan", "1 tablet", "With water"), scheduledAt, scheduledAt))).getFirst();
    }
    @Test void overdueIntakeAutomaticallyBecomesOneCaseAndOneDefinitiveOmission() {
        var now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        var due = intake(now.minusSeconds(3600));
        var future = intake(now.plusSeconds(3600));
        scheduler.evaluate(now); scheduler.evaluate(now);
        var omission = cases.findByIntakeId(due.id()).orElseThrow();
        assertEquals(due.scheduledAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS), omission.getGracePeriod().startsAt());
        assertNotNull(intakes.findById(due.id()).orElseThrow().unconfirmedReportedAt());
        assertTrue(cases.findByIntakeId(future.id()).isEmpty());
        evaluate.handle(new EvaluateGracePeriodCommand(now));
        evaluate.handle(new EvaluateGracePeriodCommand(now));
        assertEquals(IntakeStatus.OMITTED, intakes.findById(due.id()).orElseThrow().status());
        assertEquals(1, cases.findByIntakeId(due.id()).orElseThrow().getAlerts().size());
        assertThrows(IntakeApplicationException.class, () -> confirm.handle(new ConfirmIntakeCommand(due.id(), ConfirmationChannel.TOUCH)));
    }
    @Test void confirmationInsideGraceResolvesCaseAndSurvivesLaterEvaluation() {
        var now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        var due = intake(now.minusSeconds(60));
        report.handle(due.id(), now);
        confirm.handle(new ConfirmIntakeCommand(due.id(), ConfirmationChannel.TOUCH));
        evaluate.handle(new EvaluateGracePeriodCommand(now.plusSeconds(3600)));
        assertEquals(IntakeStatus.CONFIRMED, intakes.findById(due.id()).orElseThrow().status());
        assertEquals(OmissionCaseStatus.RESOLVED, cases.findByIntakeId(due.id()).orElseThrow().getStatus());
        assertTrue(cases.findByIntakeId(due.id()).orElseThrow().getAlerts().isEmpty());
    }
}
