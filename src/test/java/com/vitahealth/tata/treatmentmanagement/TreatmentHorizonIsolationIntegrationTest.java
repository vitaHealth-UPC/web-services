package com.vitahealth.tata.treatmentmanagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.treatmentmanagement.application.events.TreatmentScheduleChangedEvent;
import com.vitahealth.tata.treatmentmanagement.application.internal.commandservices.ExtendActiveTreatmentSchedulesCommandHandler;
import com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.events.TreatmentScheduleEventPublisher;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Proves a failed middle candidate rolls back its consumer write while other candidates commit. */
@SpringBootTest
@ActiveProfiles("test")
class TreatmentHorizonIsolationIntegrationTest {
  @Autowired ExtendActiveTreatmentSchedulesCommandHandler extension;
  @Autowired MedicationRepository medications;
  @Autowired TreatmentRepository treatments;
  @MockitoBean TreatmentScheduleEventPublisher publisher;

  @Test
  void failedCandidateDoesNotUndoPreviousOrPreventFollowingCommittedConsumerWrites() {
    var seeded = new ArrayList<Treatment>();
    for (int index = 0; index < 3; index++) {
      var adult = UUID.randomUUID().toString();
      var medication =
          medications.save(Medication.register(adult, "Original", "Comprimido", Instant.now()));
      var treatment = Treatment.create(adult, "Presión", Instant.now());
      treatment.configure(
          new TreatmentRegimen(
              medication.id(), "1 comprimido", "DAILY", List.of(LocalTime.NOON), "", 0));
      treatment.activate();
      seeded.add(treatments.save(treatment));
    }
    seeded.sort(Comparator.comparing(Treatment::id));
    var failed = seeded.get(1).id();
    var attempts = new ArrayList<String>();
    doAnswer(
            invocation -> {
              assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
              var event = invocation.getArgument(0, TreatmentScheduleChangedEvent.class);
              attempts.add(event.treatmentId());
              var medication = medications.findById(event.medicationId()).orElseThrow();
              medication.update("Consumer wrote", medication.presentation());
              medications.save(medication);
              if (event.treatmentId().equals(failed))
                throw new IllegalStateException("test consumer failure after write");
              return null;
            })
        .when(publisher)
        .publish(any());
    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    var outcome = extension.handle();
    assertEquals(2, outcome.published());
    assertEquals(1, outcome.failed());
    assertEquals(0, outcome.skipped());
    assertEquals(seeded.stream().map(Treatment::id).toList(), attempts);
    for (var treatment : seeded) {
      var medication = medications.findById(treatment.regimen().medicationId()).orElseThrow();
      assertEquals(
          treatment.id().equals(failed) ? "Original" : "Consumer wrote", medication.name());
    }
    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
  }
}
