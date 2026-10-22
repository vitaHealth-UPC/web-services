package com.vitahealth.tata.treatmentmanagement.application.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.events.TreatmentScheduleChangedEvent;
import com.vitahealth.tata.treatmentmanagement.application.internal.commandservices.TreatmentCommandServiceImpl;
import com.vitahealth.tata.treatmentmanagement.application.internal.fakes.InMemoryMedicationRepository;
import com.vitahealth.tata.treatmentmanagement.application.internal.fakes.InMemoryTreatmentRepository;
import com.vitahealth.tata.treatmentmanagement.application.internal.queryservices.TreatmentQueryServiceImpl;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ChangeTreatmentStatusCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ConfigureTreatmentCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.CreateTreatmentCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.DeactivateMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.RegisterMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.UpdateMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetTreatmentQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.ListMedicationsQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.ListTreatmentsQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import java.time.Clock;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Keeps treatments, medications and the published schedule consistent with each other. */
class TreatmentConsistencyTest {
  private static final String CAREGIVER = "caregiver-1";
  private static final String OLDER_ADULT = "adult-1";

  private final List<TreatmentScheduleChangedEvent> events = new ArrayList<>();
  private TreatmentCommandServiceImpl commands;
  private TreatmentQueryServiceImpl queries;

  @BeforeEach
  void setUp() {
    var medications = new InMemoryMedicationRepository();
    var treatments = new InMemoryTreatmentRepository();
    commands =
        TreatmentCommandServiceFixture.create(
            medications,
            treatments,
            (caregiverId, olderAdultId) -> true,
            events::add,
            Clock.systemUTC());
    queries =
        TreatmentQueryServiceFixture.create(
            medications, treatments, (caregiverId, olderAdultId) -> true);
  }

  private String medication(String name) {
    return commands
        .registerMedication(new RegisterMedicationCommand(CAREGIVER, OLDER_ADULT, name, "50 mg"))
        .toOptional()
        .orElseThrow()
        .id();
  }

  private String activeTreatment(String medicationId) {
    var treatmentId =
        commands
            .createTreatment(new CreateTreatmentCommand(CAREGIVER, OLDER_ADULT, "Presión"))
            .toOptional()
            .orElseThrow()
            .id();
    commands
        .configureTreatment(
            new ConfigureTreatmentCommand(
                CAREGIVER,
                treatmentId,
                medicationId,
                "1 comprimido",
                "DAILY",
                List.of(LocalTime.of(8, 0)),
                "Con agua",
                10))
        .toOptional()
        .orElseThrow();
    commands
        .activate(new ChangeTreatmentStatusCommand(CAREGIVER, treatmentId))
        .toOptional()
        .orElseThrow();
    events.clear();
    return treatmentId;
  }

  @Test
  void deactivatingTheMedicationPausesTheActiveTreatmentAndTellsIntakeExecution() {
    var medicationId = medication("Losartán");
    var treatmentId = activeTreatment(medicationId);

    commands
        .deactivateMedication(new DeactivateMedicationCommand(CAREGIVER, medicationId))
        .toOptional()
        .orElseThrow();

    assertEquals(
        TreatmentStatus.PAUSED,
        queries
            .handle(new GetTreatmentQuery(CAREGIVER, treatmentId))
            .toOptional()
            .orElseThrow()
            .status());
    assertEquals(1, events.size());
    assertFalse(events.getFirst().active());
    assertEquals(treatmentId, events.getFirst().treatmentId());
  }

  @Test
  void deactivatingAMedicationNobodyUsesPublishesNothing() {
    var medicationId = medication("Losartán");

    commands
        .deactivateMedication(new DeactivateMedicationCommand(CAREGIVER, medicationId))
        .toOptional()
        .orElseThrow();

    assertTrue(events.isEmpty());
  }

  @Test
  void renamingTheMedicationRepublishesTheScheduleWithTheNewName() {
    var medicationId = medication("Losartán");
    activeTreatment(medicationId);

    commands
        .updateMedication(
            new UpdateMedicationCommand(CAREGIVER, medicationId, "Losartán Potásico", "100 mg"))
        .toOptional()
        .orElseThrow();

    assertEquals(1, events.size());
    assertEquals("Losartán Potásico", events.getFirst().medicationName());
    assertTrue(events.getFirst().active());
  }

  @Test
  void aTreatmentWhoseMedicationWasDeactivatedCannotBeResumed() {
    var medicationId = medication("Losartán");
    var treatmentId = activeTreatment(medicationId);
    commands
        .deactivateMedication(new DeactivateMedicationCommand(CAREGIVER, medicationId))
        .toOptional()
        .orElseThrow();

    var exception =
        failure(commands.resume(new ChangeTreatmentStatusCommand(CAREGIVER, treatmentId)));

    assertEquals(
        TreatmentApplicationException.Code.MEDICATION_INACTIVE.name(), exception.code().toString());
  }

  @Test
  void aDraftTreatmentCannotBeActivatedWithoutRegimen() {
    var treatmentId =
        commands
            .createTreatment(new CreateTreatmentCommand(CAREGIVER, OLDER_ADULT, "Presión"))
            .toOptional()
            .orElseThrow()
            .id();

    var exception =
        failure(commands.activate(new ChangeTreatmentStatusCommand(CAREGIVER, treatmentId)));

    assertEquals(
        TreatmentApplicationException.Code.INCOMPLETE_TREATMENT.name(),
        exception.code().toString());
    assertTrue(events.isEmpty());
  }

  @Test
  void configuringAnInactiveMedicationIsRejected() {
    var medicationId = medication("Losartán");
    commands
        .deactivateMedication(new DeactivateMedicationCommand(CAREGIVER, medicationId))
        .toOptional()
        .orElseThrow();
    var treatmentId =
        commands
            .createTreatment(new CreateTreatmentCommand(CAREGIVER, OLDER_ADULT, "Presión"))
            .toOptional()
            .orElseThrow()
            .id();

    var exception =
        failure(
            commands.configureTreatment(
                new ConfigureTreatmentCommand(
                    CAREGIVER,
                    treatmentId,
                    medicationId,
                    "1 comprimido",
                    "DAILY",
                    List.of(LocalTime.of(8, 0)),
                    "",
                    0)));

    assertEquals(
        TreatmentApplicationException.Code.MEDICATION_INACTIVE.name(), exception.code().toString());
  }

  @Test
  void listsOnlyTheMedicationsAndTreatmentsOfTheRequestedOlderAdult() {
    medication("Losartán");
    medication("Atorvastatina");
    commands
        .registerMedication(
            new RegisterMedicationCommand(CAREGIVER, "adult-2", "Metformina", "850 mg"))
        .toOptional()
        .orElseThrow();
    commands
        .createTreatment(new CreateTreatmentCommand(CAREGIVER, OLDER_ADULT, "Presión"))
        .toOptional()
        .orElseThrow();
    commands
        .createTreatment(new CreateTreatmentCommand(CAREGIVER, "adult-2", "Diabetes"))
        .toOptional()
        .orElseThrow();

    assertEquals(
        2,
        queries
            .handle(new ListMedicationsQuery(CAREGIVER, OLDER_ADULT))
            .toOptional()
            .orElseThrow()
            .size());
    assertEquals(
        1,
        queries
            .handle(new ListTreatmentsQuery(CAREGIVER, OLDER_ADULT))
            .toOptional()
            .orElseThrow()
            .size());
    assertEquals(
        "Diabetes",
        queries
            .handle(new ListTreatmentsQuery(CAREGIVER, "adult-2"))
            .toOptional()
            .orElseThrow()
            .getFirst()
            .name());
  }

  @Test
  void listingRequiresAnActiveCareLink() {
    var denied =
        TreatmentQueryServiceFixture.create(
            new InMemoryMedicationRepository(),
            new InMemoryTreatmentRepository(),
            (caregiverId, olderAdultId) -> false);

    assertEquals(
        TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED.name(),
        failure(denied.handle(new ListMedicationsQuery("x", OLDER_ADULT))).code().toString());
    assertEquals(
        TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED.name(),
        failure(denied.handle(new ListTreatmentsQuery("x", OLDER_ADULT))).code().toString());
  }

  /** Asserts a public expected failure without relying on an exception transport. */
  private static ApplicationError failure(Result<?, ApplicationError> result) {
    assertInstanceOf(Result.Failure.class, result);
    return ((Result.Failure<?, ApplicationError>) result).error();
  }
}
