package com.vitahealth.tata.treatmentmanagement.application.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.internal.fakes.InMemoryMedicationRepository;
import com.vitahealth.tata.treatmentmanagement.application.internal.fakes.InMemoryTreatmentRepository;
import com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.events.TreatmentScheduleEventPublisher;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.CreateTreatmentCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.DeactivateMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.RegisterMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.UpdateMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetMedicationQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetTreatmentQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TreatmentAuthorizationTest {

  @Test
  void activeCareLinkIsRequiredToCreateTreatment() {
    var service =
        TreatmentCommandServiceFixture.create(
            new InMemoryMedicationRepository(),
            new InMemoryTreatmentRepository(),
            (caregiverId, olderAdultId) -> false,
            mock(TreatmentScheduleEventPublisher.class),
            Clock.systemUTC());

    var exception =
        failure(
            service.createTreatment(
                new CreateTreatmentCommand("caregiver-1", "adult-1", "Control de presión")));

    assertEquals(
        TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED.name(),
        exception.code().toString());
  }

  @Test
  void activeCareLinkCreatesDraftTreatmentForOlderAdult() {
    var service =
        TreatmentCommandServiceFixture.create(
            new InMemoryMedicationRepository(),
            new InMemoryTreatmentRepository(),
            (caregiverId, olderAdultId) ->
                caregiverId.equals("caregiver-1") && olderAdultId.equals("adult-1"),
            mock(TreatmentScheduleEventPublisher.class),
            Clock.systemUTC());

    var result =
        service
            .createTreatment(
                new CreateTreatmentCommand("caregiver-1", "adult-1", "Control de presión"))
            .toOptional()
            .orElseThrow();

    assertEquals("adult-1", result.olderAdultId());
    assertEquals(TreatmentStatus.DRAFT, result.status());
  }

  @Test
  void activeCareLinkIsRequiredToRegisterMedication() {
    var service =
        TreatmentCommandServiceFixture.create(
            new InMemoryMedicationRepository(),
            new InMemoryTreatmentRepository(),
            (caregiverId, olderAdultId) -> false,
            mock(TreatmentScheduleEventPublisher.class),
            Clock.systemUTC());

    var exception =
        failure(
            service.registerMedication(
                new RegisterMedicationCommand("caregiver-1", "adult-1", "Losartán", "50 mg")));

    assertEquals(
        TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED.name(),
        exception.code().toString());
  }

  @Test
  void medicationMutationAndDetailRejectCaregiverWithoutActiveLink() {
    var medications = new InMemoryMedicationRepository();
    var medication =
        Medication.register("adult-1", "Losartán", "50 mg", Instant.parse("2026-10-05T12:00:00Z"));
    medications.save(medication);

    var service =
        TreatmentCommandServiceFixture.create(
            medications,
            new InMemoryTreatmentRepository(),
            (caregiverId, olderAdultId) -> false,
            mock(TreatmentScheduleEventPublisher.class),
            Clock.systemUTC());
    var queries =
        TreatmentQueryServiceFixture.create(
            medications, new InMemoryTreatmentRepository(), (caregiverId, olderAdultId) -> false);

    assertEquals(
        TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED.name(),
        failure(
                service.updateMedication(
                    new UpdateMedicationCommand(
                        "caregiver-2", medication.id(), "Losartán", "100 mg")))
            .code()
            .toString());
    assertEquals(
        TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED.name(),
        failure(
                service.deactivateMedication(
                    new DeactivateMedicationCommand("caregiver-2", medication.id())))
            .code()
            .toString());
    assertEquals(
        TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED.name(),
        failure(queries.handle(new GetMedicationQuery("caregiver-2", medication.id())))
            .code()
            .toString());
  }

  @Test
  void treatmentDetailRejectsCaregiverWithoutActiveLink() {
    var treatments = new InMemoryTreatmentRepository();
    var treatment =
        Treatment.create("adult-1", "Control de presión", Instant.parse("2026-10-05T12:00:00Z"));
    treatments.save(treatment);

    var queries =
        TreatmentQueryServiceFixture.create(
            new InMemoryMedicationRepository(), treatments, (caregiverId, olderAdultId) -> false);

    var exception = failure(queries.handle(new GetTreatmentQuery("caregiver-2", treatment.id())));

    assertEquals(
        TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED.name(),
        exception.code().toString());
  }

  /** Asserts a public expected failure without relying on an exception transport. */
  private static ApplicationError failure(Result<?, ApplicationError> result) {
    assertInstanceOf(Result.Failure.class, result);
    return ((Result.Failure<?, ApplicationError>) result).error();
  }
}
