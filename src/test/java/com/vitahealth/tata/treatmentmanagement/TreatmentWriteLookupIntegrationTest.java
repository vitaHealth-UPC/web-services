package com.vitahealth.tata.treatmentmanagement;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifies write lookup requires a caller transaction and refreshes stale persistence snapshots.
 */
@SpringBootTest
@ActiveProfiles("test")
class TreatmentWriteLookupIntegrationTest {
  @Autowired MedicationRepository medications;
  @Autowired TreatmentRepository treatments;
  @Autowired PlatformTransactionManager transactions;

  @Test
  void rejectsWriteLookupOutsideACallerTransaction() {
    assertThrows(
        IllegalTransactionStateException.class, () -> medications.findForUpdate("missing"));
    assertThrows(IllegalTransactionStateException.class, () -> treatments.findForUpdate("missing"));
  }

  @Test
  void lockingMedicationLookupRefreshesAnEarlierManagedSnapshot() {
    var medication =
        medications.save(Medication.register("adult", "Losartán", "50 mg", Instant.now()));
    var outer = new TransactionTemplate(transactions);
    var independent = new TransactionTemplate(transactions);
    independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    outer.executeWithoutResult(
        status -> {
          assertTrue(medications.findById(medication.id()).orElseThrow().active());
          independent.executeWithoutResult(
              other -> {
                var changed = medications.findById(medication.id()).orElseThrow();
                changed.deactivate();
                medications.save(changed);
              });
          assertFalse(medications.findForUpdate(medication.id()).orElseThrow().active());
        });
  }

  @Test
  void lockedTreatmentLookupPreservesIdentityAndAbsence() {
    var treatment = treatments.save(Treatment.create("adult", "Presión", Instant.now()));
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              assertEquals(
                  treatment.id(), treatments.findForUpdate(treatment.id()).orElseThrow().id());
              assertTrue(treatments.findForUpdate("missing").isEmpty());
              assertTrue(medications.findForUpdate("missing").isEmpty());
            });
  }

  @Test
  void cursorPageDoesNotSkipFollowingEvidenceWhenAnEarlierCandidateIsPaused() {
    var regimen =
        new TreatmentRegimen("medication", "1 comprimido", "DAILY", List.of(LocalTime.NOON), "", 0);
    var prefix = "zz-page-";
    var first =
        treatments.save(
            Treatment.rehydrate(
                prefix + "a", "adult", "A", TreatmentStatus.ACTIVE, regimen, Instant.now()));
    treatments.save(
        Treatment.rehydrate(
            prefix + "b", "adult", "B", TreatmentStatus.ACTIVE, regimen, Instant.now()));
    treatments.save(
        Treatment.rehydrate(
            prefix + "c", "adult", "C", TreatmentStatus.ACTIVE, regimen, Instant.now()));
    assertEquals(
        List.of(prefix + "a", prefix + "b"),
        treatments.findIdsByStatusAfter(TreatmentStatus.ACTIVE, prefix, 2));
    first.pause();
    treatments.save(first);
    assertEquals(
        List.of(prefix + "c"),
        treatments.findIdsByStatusAfter(TreatmentStatus.ACTIVE, prefix + "b", 2));
    assertTrue(treatments.findIdsByStatusAfter(TreatmentStatus.ACTIVE, prefix + "c", 2).isEmpty());
    assertThrows(
        InvalidDataAccessApiUsageException.class,
        () -> treatments.findIdsByStatusAfter(TreatmentStatus.ACTIVE, "", 0));
    assertThrows(
        InvalidDataAccessApiUsageException.class,
        () -> treatments.findIdsByStatusAfter(TreatmentStatus.ACTIVE, "", 501));
  }
}
