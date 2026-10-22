package com.vitahealth.tata.treatmentmanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRecurrence;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Forces a database round trip instead of verifying only the persistence assembler. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TreatmentRecurrenceJpaIntegrationTest {
  @Autowired TreatmentRepository treatments;
  @Autowired EntityManager entities;

  @Test
  void preservesZoneAndAnchorAfterFlushAndPersistenceContextClear() {
    var recurrence =
        new TreatmentRecurrence(
            TreatmentRecurrence.Mode.FIXED_INTERVAL,
            "America/Bogota",
            Instant.parse("2026-10-21T13:00:00.123456Z"));
    var treatment = Treatment.create("adult", "Presión", Instant.EPOCH);
    treatment.configure(
        new TreatmentRegimen(
            "medication",
            "1 comprimido",
            "cada 12 horas",
            List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            "",
            10,
            recurrence));
    treatments.save(treatment);
    entities.flush();
    entities.clear();
    var restored = treatments.findById(treatment.id()).orElseThrow();
    assertEquals(treatment.regimen(), restored.regimen());
  }
}
