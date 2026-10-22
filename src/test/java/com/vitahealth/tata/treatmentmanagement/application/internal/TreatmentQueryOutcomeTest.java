package com.vitahealth.tata.treatmentmanagement.application.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetMedicationQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetTreatmentQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.ListMedicationsQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.ListTreatmentsQuery;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Distinguishes empty successful lists, missing required evidence and technical query failures. */
class TreatmentQueryOutcomeTest {
  @Test
  void missingRequiredEvidenceReturnsStableFailures() {
    var medications = mock(MedicationRepository.class);
    var treatments = mock(TreatmentRepository.class);
    when(medications.findById("missing")).thenReturn(Optional.empty());
    when(treatments.findById("missing")).thenReturn(Optional.empty());
    var queries =
        TreatmentQueryServiceFixture.create(medications, treatments, (caregiver, adult) -> true);
    assertEquals(
        "MEDICATION_NOT_FOUND",
        failure(queries.handle(new GetMedicationQuery("caregiver", "missing"))).code());
    assertEquals(
        "TREATMENT_NOT_FOUND",
        failure(queries.handle(new GetTreatmentQuery("caregiver", "missing"))).code());
  }

  @Test
  void authorizedEmptyListsAreSuccessfulValues() {
    var queries =
        TreatmentQueryServiceFixture.create(
            mock(MedicationRepository.class),
            mock(TreatmentRepository.class),
            (caregiver, adult) -> true);
    assertTrue(
        queries
            .handle(new ListMedicationsQuery("caregiver", "adult"))
            .toOptional()
            .orElseThrow()
            .isEmpty());
    assertTrue(
        queries
            .handle(new ListTreatmentsQuery("caregiver", "adult"))
            .toOptional()
            .orElseThrow()
            .isEmpty());
  }

  @Test
  void technicalRepositoryAndAuthorizationFailuresPropagateUnchanged() {
    var medications = mock(MedicationRepository.class);
    var treatments = mock(TreatmentRepository.class);
    var failure = new IllegalStateException("persistence unavailable");
    when(medications.findById("medication")).thenThrow(failure);
    var queries =
        TreatmentQueryServiceFixture.create(medications, treatments, (caregiver, adult) -> true);
    assertSame(
        failure,
        assertThrows(
            IllegalStateException.class,
            () -> queries.handle(new GetMedicationQuery("caregiver", "medication"))));
    var denied =
        TreatmentQueryServiceFixture.create(
            medications,
            treatments,
            (caregiver, adult) -> {
              throw failure;
            });
    assertSame(
        failure,
        assertThrows(
            IllegalStateException.class,
            () -> denied.handle(new ListTreatmentsQuery("caregiver", "adult"))));
    verifyNoInteractions(treatments);
  }

  private static ApplicationError failure(Result<?, ApplicationError> result) {
    assertInstanceOf(Result.Failure.class, result);
    return ((Result.Failure<?, ApplicationError>) result).error();
  }
}
