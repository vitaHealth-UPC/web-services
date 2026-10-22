package com.vitahealth.tata.treatmentmanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.interfaces.acl.TreatmentContextFacade;
import java.time.Clock;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Verifies bounded source discovery uses stable identity order and actual medication ownership. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TreatmentMedicationReferencePagingTest {
  @Autowired TreatmentContextFacade treatments;
  @Autowired MedicationRepository medications;
  @Autowired Clock clock;

  @Test
  void pagesAcrossOneHundredOwnedReferencesWithoutForeignEntriesOrDuplicates() {
    var adult = UUID.randomUUID().toString();
    var owned = new ArrayList<String>();
    for (int i = 0; i < 105; i++) {
      owned.add(
          medications
              .save(Medication.register(adult, "Medication " + i, "Tablet", clock.instant()))
              .id());
    }
    medications.save(
        Medication.register(UUID.randomUUID().toString(), "Foreign", "Tablet", clock.instant()));
    var expected = owned.stream().sorted().toList();
    var first = treatments.findMedicationIdsAfter(adult, "", 100);
    var last = treatments.findMedicationIdsAfter(adult, first.getLast(), 100);
    assertEquals(expected.subList(0, 100), first);
    assertEquals(expected.subList(100, 105), last);
    assertTrue(treatments.findMedicationIdsAfter(adult, last.getLast(), 100).isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> first.add("unexpected"));
    assertEquals(expected.subList(0, 1), treatments.findMedicationIdsAfter(adult, "", 1));
  }

  @Test
  void rejectsUnboundedOrMissingSourceArguments() {
    var adult = UUID.randomUUID().toString();
    assertThrows(
        IllegalArgumentException.class, () -> treatments.findMedicationIdsAfter(adult, "", 0));
    assertThrows(
        IllegalArgumentException.class, () -> treatments.findMedicationIdsAfter(adult, "", 101));
    assertThrows(
        IllegalArgumentException.class, () -> treatments.findMedicationIdsAfter(" ", "", 1));
    assertThrows(
        NullPointerException.class, () -> treatments.findMedicationIdsAfter(adult, null, 1));
  }
}
