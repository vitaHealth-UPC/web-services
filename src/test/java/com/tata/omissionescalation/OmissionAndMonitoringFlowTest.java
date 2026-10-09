package com.tata.omissionescalation;

import static org.assertj.core.api.Assertions.assertThat;

import com.tata.familymonitoring.application.internal.commandservices.CloseAlertCommandHandler;
import com.tata.familymonitoring.application.internal.commandservices.CreateCaregiverNoteCommandHandler;
import com.tata.familymonitoring.application.internal.commandservices.MarkAlertAttendedCommandHandler;
import com.tata.familymonitoring.application.internal.queryservices.GetCaregiverNotesQueryHandler;
import com.tata.familymonitoring.application.internal.queryservices.GetOlderAdultStatusQueryHandler;
import com.tata.familymonitoring.application.queryservices.OlderAdultStatusView;
import com.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.tata.familymonitoring.domain.model.commands.CloseAlertCommand;
import com.tata.familymonitoring.domain.model.commands.CreateCaregiverNoteCommand;
import com.tata.familymonitoring.domain.model.commands.MarkAlertAttendedCommand;
import com.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.tata.familymonitoring.domain.model.queries.GetCaregiverNotesQuery;
import com.tata.familymonitoring.domain.model.queries.GetOlderAdultStatusQuery;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import com.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import com.tata.intakeexecution.domain.model.events.IntakeUnconfirmed;
import com.tata.omissionescalation.application.internal.commandservices.EvaluateGracePeriodCommandHandler;
import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.commands.EvaluateGracePeriodCommand;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the real flow against H2: intake events in, omission cases, alerts and escalation, and the
 * caregiver-facing side. The test profile uses a zero-length grace period.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OmissionAndMonitoringFlowTest {

  @Autowired ApplicationEventPublisher events;
  @Autowired com.tata.intakeexecution.domain.repositories.IntakeRepository intakes;
  @Autowired com.tata.intakeexecution.application.commandservices.ConfirmIntakeCommandService confirmation;
  @Autowired IOmissionCaseRepository omissionCases;
  @Autowired IFamilyMonitorRepository monitors;
  @Autowired EvaluateGracePeriodCommandHandler evaluateHandler;
  @Autowired GetOlderAdultStatusQueryHandler statusHandler;
  @Autowired MarkAlertAttendedCommandHandler markAttendedHandler;
  @Autowired CloseAlertCommandHandler closeAlertHandler;
  @Autowired CreateCaregiverNoteCommandHandler createNoteHandler;
  @Autowired GetCaregiverNotesQueryHandler notesHandler;

  private static long sequence = 1000;

  private String nextId() {
    return new java.util.UUID(0, ++sequence).toString();
  }

  private void givenMonitorFor(String olderAdultId) {
    if (monitors.findByOlderAdultId(olderAdultId).isEmpty()) {
      monitors.save(new FamilyMonitor(olderAdultId, olderAdultId, "account-5"));
    }
  }

  private void unconfirmed(String intakeId, String olderAdultId) {
    var scheduledAt = Instant.now().minusSeconds(120);
    if (intakes.findById(intakeId).isEmpty()) {
      intakes.saveAll(java.util.List.of(com.tata.intakeexecution.domain.model.aggregates.Intake.rehydrate(
          intakeId, nextId(), nextId(), olderAdultId,
          new com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot("Losartan 50 mg", "1 comprimido", "Con agua"),
          scheduledAt, com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus.PENDING, scheduledAt)));
    }
    events.publishEvent(new IntakeUnconfirmed(intakeId, olderAdultId, "Losartan 50 mg", scheduledAt));
  }

  @Test
  void unconfirmedIntake_opensAPendingCaseOnlyOnce() {
    String intakeId = nextId();

    unconfirmed(intakeId, "adult-1");
    unconfirmed(intakeId, "adult-1");

    OmissionCase omissionCase = omissionCases.findByIntakeId(intakeId).orElseThrow();
    assertThat(omissionCase.getStatus()).isEqualTo(OmissionCaseStatus.PENDING);
    assertThat(omissionCase.getReinforcedReminderSentAt()).isNotNull();
  }

  @Test
  void expiredCase_becomesOneOmissionWithOneAlert_evenIfEvaluatedTwice() {
    String olderAdultId = nextId();
    String intakeId = nextId();
    unconfirmed(intakeId, olderAdultId);
    Instant now = Instant.now().plusSeconds(5);

    evaluateHandler.handle(new EvaluateGracePeriodCommand(now));
    evaluateHandler.handle(new EvaluateGracePeriodCommand(now.plusSeconds(60)));

    OmissionCase omissionCase = omissionCases.findByIntakeId(intakeId).orElseThrow();
    assertThat(omissionCase.getStatus()).isEqualTo(OmissionCaseStatus.OMITTED);
    assertThat(omissionCase.getAlerts()).hasSize(1);
    assertThat(omissionCase.getAlerts().getFirst().getStatus())
        .isEqualTo(com.tata.omissionescalation.domain.model.valueobjects.AlertStatus.SENT);
    assertThat(omissionCase.getEscalations()).isEmpty();
  }

  @Test
  void omittedCase_escalatesThroughTheLevelsAndEndsClosed() {
    String olderAdultId = nextId();
    String intakeId = nextId();
    unconfirmed(intakeId, olderAdultId);
    Instant now = Instant.now().plusSeconds(5);
    evaluateHandler.handle(new EvaluateGracePeriodCommand(now));

    for (int step = 1; step <= 3; step++) {
      evaluateHandler.handle(new EvaluateGracePeriodCommand(now.plus(Duration.ofMinutes(31L * step))));
    }

    OmissionCase omissionCase = omissionCases.findByIntakeId(intakeId).orElseThrow();
    assertThat(omissionCase.getEscalations()).hasSize(3);
    assertThat(omissionCase.getStatus()).isEqualTo(OmissionCaseStatus.CLOSED);
  }

  @Test
  void confirmationEventWithoutRecordedEvidence_doesNotResolveTheCase() {
    String intakeId = nextId();
    unconfirmed(intakeId, nextId());

    events.publishEvent(new IntakeConfirmed(intakeId, "medication-1", "adult-1", Instant.now()));

    assertThat(omissionCases.findByIntakeId(intakeId).orElseThrow().getStatus())
        .isEqualTo(OmissionCaseStatus.PENDING);
  }

  @Test
  void omission_reachesTheCaregiver_whoCanAttendAndCloseTheAlert() {
    String olderAdultId = nextId();
    givenMonitorFor(olderAdultId);
    unconfirmed(nextId(), olderAdultId);
    evaluateHandler.handle(new EvaluateGracePeriodCommand(Instant.now().plusSeconds(5)));

    OlderAdultStatusView view = statusHandler.handle(new GetOlderAdultStatusQuery(olderAdultId));
    assertThat(view.status().hasOpenAlert()).isTrue();
    assertThat(view.status().nextIntakeAt()).isNull();
    assertThat(view.status().lastIntakeStatus().name()).isEqualTo("OMITTED");
    AlertSummary alert = view.openAlerts().getFirst();

    AlertSummary attended =
        markAttendedHandler.handle(new MarkAlertAttendedCommand(olderAdultId, alert.getId()));
    assertThat(attended.getStatus()).isEqualTo(com.tata.familymonitoring.domain.model.valueobjects.AlertStatus.ATTENDED);

    AlertSummary closed =
        closeAlertHandler.handle(new CloseAlertCommand(olderAdultId, alert.getId()));
    assertThat(closed.getStatus()).isEqualTo(com.tata.familymonitoring.domain.model.valueobjects.AlertStatus.CLOSED);
    assertThat(statusHandler.handle(new GetOlderAdultStatusQuery(olderAdultId))
        .status().hasOpenAlert()).isFalse();
  }

  @Test
  void note_isStoredWithAuthorAndTime_andStaysAvailable() {
    String olderAdultId = nextId();
    givenMonitorFor(olderAdultId);

    createNoteHandler.handle(new CreateCaregiverNoteCommand(olderAdultId, "account-5", "Called her"));

    var notes = notesHandler.handle(new GetCaregiverNotesQuery(olderAdultId));
    assertThat(notes).hasSize(1);
    assertThat(notes.getFirst().getId()).isNotNull();
    assertThat(notes.getFirst().getFamiliarId()).isEqualTo("account-5");
    assertThat(notes.getFirst().getRecordedAt()).isNotNull();
  }
  @Test
  void confirmedSourceIntakeCannotProduceAnOmissionOrCaregiverAlert() {
    var adultId = nextId();
    givenMonitorFor(adultId);
    var intakeId = nextId();
    unconfirmed(intakeId, adultId);
    confirmation.handle(new com.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand(
        intakeId, com.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel.TOUCH));
    var recorded = intakes.findById(intakeId).orElseThrow();
    events.publishEvent(new IntakeConfirmed(intakeId, recorded.medicationId(), adultId, recorded.confirmedAt()));
    evaluateHandler.handle(new EvaluateGracePeriodCommand(Instant.now().plusSeconds(5)));
    assertThat(omissionCases.findByIntakeId(intakeId).orElseThrow().getOmittedAt()).isNull();
    assertThat(omissionCases.findByIntakeId(intakeId).orElseThrow().getStatus()).isEqualTo(OmissionCaseStatus.RESOLVED);
    assertThat(monitors.findByOlderAdultId(adultId).orElseThrow().getAlerts()).isEmpty();
    assertThat(intakes.findById(intakeId).orElseThrow().status())
        .isIn(com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus.CONFIRMED,
              com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus.LATE);
  }
}
