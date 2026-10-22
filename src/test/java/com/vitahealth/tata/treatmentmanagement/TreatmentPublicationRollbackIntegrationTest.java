package com.vitahealth.tata.treatmentmanagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.commandservices.TreatmentCommandService;
import com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.CareLinkVerificationPort;
import com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.events.TreatmentScheduleEventPublisher;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ChangeTreatmentStatusCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ConfigureTreatmentCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.CreateTreatmentCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.RegisterMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies handler rollback using committed setup and fresh repository reads, without a test
 * transaction.
 */
@SpringBootTest
@ActiveProfiles("test")
class TreatmentPublicationRollbackIntegrationTest {
  @Autowired TreatmentCommandService commands;
  @Autowired TreatmentRepository treatments;
  @MockitoBean CareLinkVerificationPort careLinks;
  @MockitoBean TreatmentScheduleEventPublisher publication;

  @Test
  void latePublicationFailureRollsBackRegimenAndActivation() {
    when(careLinks.isAuthorized(anyString(), anyString())).thenReturn(true);
    var caregiver = UUID.randomUUID().toString();
    var adult = UUID.randomUUID().toString();
    var medication =
        commands
            .registerMedication(
                new RegisterMedicationCommand(caregiver, adult, "Losartán", "Comprimido"))
            .toOptional()
            .orElseThrow();
    var treatment =
        commands
            .createTreatment(new CreateTreatmentCommand(caregiver, adult, "Control de presión"))
            .toOptional()
            .orElseThrow();
    var configuration =
        new ConfigureTreatmentCommand(
            caregiver,
            treatment.id(),
            medication.id(),
            "50 mg",
            "Diaria",
            List.of(LocalTime.of(8, 0)),
            "Con alimentos",
            5);
    var failure = new IllegalStateException("schedule delivery failed");
    doThrow(failure).when(publication).publish(any());
    assertSame(
        failure,
        assertThrows(
            IllegalStateException.class,
            () -> commands.configureTreatment(configuration).toOptional().orElseThrow()));
    var afterConfiguration = treatments.findById(treatment.id()).orElseThrow();
    assertNull(afterConfiguration.regimen());
    assertEquals(TreatmentStatus.DRAFT, afterConfiguration.status());

    doNothing().when(publication).publish(any());
    commands.configureTreatment(configuration).toOptional().orElseThrow();
    doThrow(failure).when(publication).publish(any());
    assertSame(
        failure,
        assertThrows(
            IllegalStateException.class,
            () ->
                commands
                    .activate(new ChangeTreatmentStatusCommand(caregiver, treatment.id()))
                    .toOptional()
                    .orElseThrow()));
    var afterActivation = treatments.findById(treatment.id()).orElseThrow();
    assertEquals(TreatmentStatus.DRAFT, afterActivation.status());
    assertEquals(medication.id(), afterActivation.regimen().medicationId());
    doThrow(
            new TreatmentApplicationException(
                TreatmentApplicationException.Code.INVALID_TRANSITION, "downstream rejection"))
        .when(publication)
        .publish(any());
    var rejected = commands.activate(new ChangeTreatmentStatusCommand(caregiver, treatment.id()));
    assertInstanceOf(Result.Failure.class, rejected);
    assertEquals(
        "INVALID_TRANSITION", ((Result.Failure<?, ApplicationError>) rejected).error().code());
    assertEquals(TreatmentStatus.DRAFT, treatments.findById(treatment.id()).orElseThrow().status());
    verify(publication, times(4)).publish(any());
  }
}
