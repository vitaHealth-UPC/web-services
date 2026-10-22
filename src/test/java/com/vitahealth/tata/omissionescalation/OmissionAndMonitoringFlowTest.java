package com.vitahealth.tata.omissionescalation;

import static com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertStatus.ATTENDED;
import static com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertStatus.CLOSED;
import static org.assertj.core.api.Assertions.assertThat;

import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories.CareLinkJpaRepository;
import com.vitahealth.tata.familymonitoring.application.internal.commandservices.CloseAlertCommandHandler;
import com.vitahealth.tata.familymonitoring.application.internal.commandservices.CreateCaregiverNoteCommandHandler;
import com.vitahealth.tata.familymonitoring.application.internal.commandservices.MarkAlertAttendedCommandHandler;
import com.vitahealth.tata.familymonitoring.application.internal.queryservices.FamilyMonitorSelectionService;
import com.vitahealth.tata.familymonitoring.application.internal.queryservices.GetCaregiverNotesQueryHandler;
import com.vitahealth.tata.familymonitoring.application.internal.queryservices.GetOlderAdultStatusQueryHandler;
import com.vitahealth.tata.familymonitoring.application.result.AlertResult;
import com.vitahealth.tata.familymonitoring.application.result.OlderAdultStatusResult;
import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.model.commands.CloseAlertCommand;
import com.vitahealth.tata.familymonitoring.domain.model.commands.CreateCaregiverNoteCommand;
import com.vitahealth.tata.familymonitoring.domain.model.commands.MarkAlertAttendedCommand;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetCaregiverNotesQuery;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetOlderAdultStatusQuery;
import com.vitahealth.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.repositories.FamilyMonitorJpaRepository;
import com.vitahealth.tata.intakeexecution.application.commandservices.ConfirmIntakeCommandService;
import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand;
import com.vitahealth.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import com.vitahealth.tata.intakeexecution.domain.model.events.IntakeUnconfirmed;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.repositories.IntakeJpaRepository;
import com.vitahealth.tata.omissionescalation.application.internal.commandservices.EvaluateGracePeriodCommandHandler;
import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.commands.EvaluateGracePeriodCommand;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationIntentJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.OmissionCaseJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs committed source and monitoring transactions against H2: intake events in, omission cases,
 * alerts and escalation, and the caregiver-facing side. The test profile uses a zero-length grace
 * period.
 */
@SpringBootTest
@ActiveProfiles("test")
class OmissionAndMonitoringFlowTest {

  @Autowired ApplicationEventPublisher events;
  @Autowired Clock clock;
  @Autowired IntakeRepository intakes;

  @Autowired ConfirmIntakeCommandService confirmation;

  @Autowired IOmissionCaseRepository omissionCases;
  @Autowired IFamilyMonitorRepository monitors;
  @Autowired FamilyMonitorSelectionService selection;
  @Autowired EvaluateGracePeriodCommandHandler evaluateHandler;
  @Autowired GetOlderAdultStatusQueryHandler statusHandler;
  @Autowired MarkAlertAttendedCommandHandler markAttendedHandler;
  @Autowired CloseAlertCommandHandler closeAlertHandler;
  @Autowired CreateCaregiverNoteCommandHandler createNoteHandler;
  @Autowired GetCaregiverNotesQueryHandler notesHandler;

  private final List<String> ownedIds = new ArrayList<>();
  @Autowired PlatformTransactionManager transactions;
  @Autowired OmissionCaseJpaRepository caseRows;
  @Autowired NotificationIntentJpaRepository intentRows;
  @Autowired FamilyMonitorJpaRepository monitorRows;
  @Autowired IntakeJpaRepository intakeRows;
  @Autowired CareLinkRepository links;
  @Autowired CareLinkJpaRepository linkRows;
  private final List<String> ownedLinks = new ArrayList<>();

  @AfterEach
  void removeCommittedFixture() {
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              for (String id : ownedIds) {
                caseRows
                    .findByIntakeId(id)
                    .ifPresent(
                        source -> {
                          for (NotificationKind kind : NotificationKind.values())
                            intentRows
                                .findBySourceCaseIdAndKind(source.getId(), kind)
                                .ifPresent(intentRows::delete);
                          intentRows.flush();
                          caseRows.delete(source);
                        });
              }
              caseRows.flush();
              for (String linkId : ownedLinks)
                monitorRows.findByCareLinkId(linkId).ifPresent(monitorRows::delete);
              monitorRows.flush();
              intakeRows.deleteAllById(ownedIds);
              linkRows.deleteAllById(ownedLinks);
            });
  }

  private String nextId() {
    String id = UUID.randomUUID().toString();
    ownedIds.add(id);
    return id;
  }

  private void givenMonitorFor(String olderAdultId) {
    if (monitors.findIdsByParticipantsAfter(olderAdultId, "account-5", 0, 1).isEmpty()) {
      var now = clock.instant();
      var code = UUID.randomUUID().toString().replace("-", "");
      var link =
          CareLink.createPending(
              "account-5", olderAdultId, LinkingCode.issue(code, now, Duration.ofMinutes(15)), now);
      link.accept(code, now);
      link.registerConsent(true, now);
      link.confirm(now);
      links.save(link);
      ownedLinks.add(link.id());
      monitors.save(new FamilyMonitor(link.id(), olderAdultId, "account-5"));
    }
  }

  private void unconfirmed(String intakeId, String olderAdultId) {
    var scheduledAt = clock.instant().minusSeconds(120);
    if (intakes.findById(intakeId).isEmpty()) {
      intakes.saveAll(
          List.of(
              Intake.rehydrate(
                  intakeId,
                  nextId(),
                  nextId(),
                  olderAdultId,
                  new MedicationSnapshot("Losartan 50 mg", "1 comprimido", "Con agua"),
                  scheduledAt,
                  IntakeStatus.PENDING,
                  scheduledAt)));
    }
    events.publishEvent(
        new IntakeUnconfirmed(intakeId, olderAdultId, "Losartan 50 mg", scheduledAt));
  }

  @Test
  void unconfirmedIntake_opensAPendingCaseOnlyOnce() {
    String intakeId = nextId();

    unconfirmed(intakeId, "adult-1");
    unconfirmed(intakeId, "adult-1");

    OmissionCase omissionCase = omissionCases.findByIntakeId(intakeId).orElseThrow();
    assertThat(omissionCase.getStatus()).isEqualTo(OmissionCaseStatus.PENDING);
    assertThat(omissionCase.getReinforcedReminderSentAt()).isNull();
  }

  @Test
  void expiredCase_becomesOneOmissionWithOneAlert_evenIfEvaluatedTwice() {
    String olderAdultId = nextId();
    String intakeId = nextId();
    unconfirmed(intakeId, olderAdultId);
    Instant now = clock.instant().plusSeconds(5);

    evaluateHandler.handle(new EvaluateGracePeriodCommand(now));
    evaluateHandler.handle(new EvaluateGracePeriodCommand(now.plusSeconds(60)));

    OmissionCase omissionCase = omissionCases.findByIntakeId(intakeId).orElseThrow();
    assertThat(omissionCase.getStatus()).isEqualTo(OmissionCaseStatus.OMITTED);
    assertThat(omissionCase.getAlerts()).hasSize(1);
    assertThat(omissionCase.getAlerts().getFirst().getStatus()).isEqualTo(AlertStatus.GENERATED);
    assertThat(omissionCase.getAlerts().getFirst().getSentAt()).isNull();
    assertThat(omissionCase.getAlerts().getFirst().getFailureReason()).isNull();
    assertThat(omissionCase.getEscalations()).isEmpty();
  }

  @Test
  void omittedCase_escalatesThroughTheLevelsAndEndsClosed() {
    String olderAdultId = nextId();
    String intakeId = nextId();
    unconfirmed(intakeId, olderAdultId);
    Instant now = clock.instant().plusSeconds(5);
    evaluateHandler.handle(new EvaluateGracePeriodCommand(now));

    for (int step = 1; step <= 3; step++) {
      evaluateHandler.handle(
          new EvaluateGracePeriodCommand(now.plus(Duration.ofMinutes(31L * step))));
    }

    OmissionCase omissionCase = omissionCases.findByIntakeId(intakeId).orElseThrow();
    assertThat(omissionCase.getEscalations()).hasSize(3);
    assertThat(omissionCase.getStatus()).isEqualTo(OmissionCaseStatus.CLOSED);
  }

  @Test
  void confirmationEventWithoutRecordedEvidence_doesNotResolveTheCase() {
    String intakeId = nextId();
    unconfirmed(intakeId, nextId());

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class,
        () ->
            new TransactionTemplate(transactions)
                .executeWithoutResult(
                    status ->
                        events.publishEvent(
                            new IntakeConfirmed(
                                intakeId, "medication-1", "adult-1", clock.instant()))));

    assertThat(omissionCases.findByIntakeId(intakeId).orElseThrow().getStatus())
        .isEqualTo(OmissionCaseStatus.PENDING);
  }

  @Test
  void omission_reachesTheCaregiver_whoCanAttendAndCloseTheAlert() {
    String olderAdultId = nextId();
    givenMonitorFor(olderAdultId);
    unconfirmed(nextId(), olderAdultId);
    evaluateHandler.handle(new EvaluateGracePeriodCommand(clock.instant()));

    OlderAdultStatusResult view =
        statusHandler.handle(new GetOlderAdultStatusQuery(olderAdultId, "account-5"));
    assertThat(view.status().hasOpenAlert()).isTrue();
    assertThat(view.status().nextIntakeAt()).isNull();
    assertThat(view.status().lastIntakeStatus().name()).isEqualTo("OMITTED");
    AlertResult alert = view.openAlerts().getFirst();

    AlertResult attended =
        markAttendedHandler.handle(
            new MarkAlertAttendedCommand(olderAdultId, "account-5", alert.id()));
    assertThat(attended.status()).isEqualTo(ATTENDED);

    AlertResult closed =
        closeAlertHandler.handle(new CloseAlertCommand(olderAdultId, "account-5", alert.id()));
    assertThat(closed.status()).isEqualTo(CLOSED);
    assertThat(
            statusHandler
                .handle(new GetOlderAdultStatusQuery(olderAdultId, "account-5"))
                .status()
                .hasOpenAlert())
        .isFalse();
  }

  @Test
  void note_isStoredWithAuthorAndTime_andStaysAvailable() {
    String olderAdultId = nextId();
    givenMonitorFor(olderAdultId);

    createNoteHandler.handle(
        new CreateCaregiverNoteCommand(olderAdultId, "account-5", "Called her"));

    var notes = notesHandler.handle(new GetCaregiverNotesQuery(olderAdultId, "account-5"));
    assertThat(notes).hasSize(1);
    assertThat(notes.getFirst().id()).isNotNull();
    assertThat(notes.getFirst().familiarId()).isEqualTo("account-5");
    assertThat(notes.getFirst().recordedAt()).isNotNull();
  }

  @Test
  void confirmedSourceIntakeCannotProduceAnOmissionOrCaregiverAlert() {
    var adultId = nextId();
    givenMonitorFor(adultId);
    var intakeId = nextId();
    unconfirmed(intakeId, adultId);
    confirmation
        .handle(new ConfirmIntakeCommand(intakeId, ConfirmationChannel.TOUCH))
        .toOptional()
        .orElseThrow();
    var recorded = intakes.findById(intakeId).orElseThrow();
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status ->
                events.publishEvent(
                    new IntakeConfirmed(
                        intakeId, recorded.medicationId(), adultId, recorded.confirmedAt())));
    evaluateHandler.handle(new EvaluateGracePeriodCommand(clock.instant().plusSeconds(5)));
    assertThat(omissionCases.findByIntakeId(intakeId).orElseThrow().getOmittedAt()).isNull();
    assertThat(omissionCases.findByIntakeId(intakeId).orElseThrow().getStatus())
        .isEqualTo(OmissionCaseStatus.RESOLVED);
    assertThat(selection.requireCurrent(adultId, "account-5").getAlerts()).isEmpty();
    assertThat(intakes.findById(intakeId).orElseThrow().status())
        .isIn(IntakeStatus.CONFIRMED, IntakeStatus.LATE);
  }

  @Test
  void delayedUnconfirmedEventCannotOpenACaseForAnAlreadyConfirmedIntake() {
    var adultId = nextId();
    givenMonitorFor(adultId);
    var intakeId = nextId();
    var scheduledAt = clock.instant().minusSeconds(120);
    intakes.saveAll(
        List.of(
            Intake.rehydrate(
                intakeId,
                nextId(),
                nextId(),
                adultId,
                new MedicationSnapshot("Losartan 50 mg", "1 comprimido", "Con agua"),
                scheduledAt,
                IntakeStatus.PENDING,
                scheduledAt)));
    confirmation
        .handle(new ConfirmIntakeCommand(intakeId, ConfirmationChannel.TOUCH))
        .toOptional()
        .orElseThrow();

    events.publishEvent(new IntakeUnconfirmed(intakeId, adultId, "Losartan 50 mg", scheduledAt));
    events.publishEvent(new IntakeUnconfirmed(intakeId, adultId, "Losartan 50 mg", scheduledAt));
    evaluateHandler.handle(new EvaluateGracePeriodCommand(clock.instant().plusSeconds(5)));

    assertThat(omissionCases.findByIntakeId(intakeId)).isEmpty();
    assertThat(selection.requireCurrent(adultId, "account-5").getAlerts()).isEmpty();
  }
}
