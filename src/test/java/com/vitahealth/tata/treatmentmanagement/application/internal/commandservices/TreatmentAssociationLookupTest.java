package com.vitahealth.tata.treatmentmanagement.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.events.TreatmentScheduleEventPublisher;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ExtendTreatmentScheduleCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Checks changed association snapshots cannot authorize writes against an unlocked medication. */
class TreatmentAssociationLookupTest {
  private final MedicationRepository medications = mock(MedicationRepository.class);
  private final TreatmentRepository treatments = mock(TreatmentRepository.class);
  private final TreatmentCommandLookupService lookup =
      new TreatmentCommandLookupService(medications, treatments);

  @Test
  void changedMedicationIsRejectedAfterMedicationThenTreatmentLock() {
    var medication = Medication.register("adult", "A", "50 mg", Instant.EPOCH);
    when(treatments.findById("treatment"))
        .thenReturn(Optional.of(configured(medication.id(), LocalTime.NOON)));
    when(medications.findForUpdate(medication.id())).thenReturn(Optional.of(medication));
    when(treatments.findForUpdate("treatment"))
        .thenReturn(Optional.of(configured("other-medication", LocalTime.NOON)));
    var failure =
        assertThrows(
            TreatmentApplicationException.class, () -> lookup.treatmentForActivation("treatment"));
    assertEquals(TreatmentApplicationException.Code.INVALID_TRANSITION, failure.code());
    var order = inOrder(medications, treatments);
    order.verify(treatments).findById("treatment");
    order.verify(medications).findForUpdate(medication.id());
    order.verify(treatments).findForUpdate("treatment");
    verifyNoMoreInteractions(medications, treatments);
  }

  @Test
  void currentRegimenIsReturnedWhenItsMedicationAssociationRemainsLocked() {
    var medication = Medication.register("adult", "A", "50 mg", Instant.EPOCH);
    var latest = configured(medication.id(), LocalTime.of(20, 0));
    when(treatments.findById("treatment"))
        .thenReturn(Optional.of(configured(medication.id(), LocalTime.NOON)));
    when(medications.findForUpdate(medication.id())).thenReturn(Optional.of(medication));
    when(treatments.findForUpdate("treatment")).thenReturn(Optional.of(latest));
    assertSame(latest, lookup.treatmentForActivation("treatment"));
    assertEquals(List.of(LocalTime.of(20, 0)), latest.regimen().scheduledTimes());
  }

  @Test
  void concurrentlyConfiguredDraftCannotBypassMedicationLock() {
    when(treatments.findById("treatment"))
        .thenReturn(
            Optional.of(
                Treatment.rehydrate(
                    "treatment", "adult", "Draft", TreatmentStatus.DRAFT, null, Instant.EPOCH)));
    when(treatments.findForUpdate("treatment"))
        .thenReturn(Optional.of(configured("new-medication", LocalTime.NOON)));
    assertEquals(
        TreatmentApplicationException.Code.INVALID_TRANSITION,
        assertThrows(
                TreatmentApplicationException.class,
                () -> lookup.treatmentForActivation("treatment"))
            .code());
    verifyNoInteractions(medications);
  }

  @Test
  void horizonSkipsChangedAssociationWithoutPublishingAnUnlockedSchedule() {
    var medication = Medication.register("adult", "A", "50 mg", Instant.EPOCH);
    var snapshot = configured(medication.id(), LocalTime.NOON);
    snapshot.activate();
    var current = configured("new-medication", LocalTime.NOON);
    current.activate();
    when(treatments.findById("treatment")).thenReturn(Optional.of(snapshot));
    when(medications.findForUpdate(medication.id())).thenReturn(Optional.of(medication));
    when(treatments.findForUpdate("treatment")).thenReturn(Optional.of(current));
    var publisher = mock(TreatmentScheduleEventPublisher.class);
    assertFalse(
        new ExtendTreatmentScheduleCommandHandler(treatments, medications, publisher)
            .handle(new ExtendTreatmentScheduleCommand("treatment")));
    verifyNoInteractions(publisher);
  }

  private static Treatment configured(String medication, LocalTime time) {
    return Treatment.rehydrate(
        "treatment",
        "adult",
        "Presión",
        TreatmentStatus.DRAFT,
        new TreatmentRegimen(medication, "1 comprimido", "DAILY", List.of(time), "", 0),
        Instant.EPOCH);
  }
}
