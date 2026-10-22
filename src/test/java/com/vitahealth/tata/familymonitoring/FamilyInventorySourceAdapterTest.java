package com.vitahealth.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.familymonitoring.application.exceptions.InvalidMonitoringSourceException;
import com.vitahealth.tata.familymonitoring.infrastructure.acl.InitialInventorySourceAdapter;
import com.vitahealth.tata.familymonitoring.infrastructure.acl.MedicationLookupAdapter;
import com.vitahealth.tata.inventoryreplenishment.application.result.InventoryStockSnapshot;
import com.vitahealth.tata.inventoryreplenishment.interfaces.acl.InventoryContextFacade;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.interfaces.acl.TreatmentContextFacade;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Verifies source contracts cannot redirect projections or hide malformed integration evidence. */
class FamilyInventorySourceAdapterTest {
  private final TreatmentContextFacade treatment = mock(TreatmentContextFacade.class);
  private final InventoryContextFacade inventory = mock(InventoryContextFacade.class);
  private final InitialInventorySourceAdapter source =
      new InitialInventorySourceAdapter(treatment, inventory);
  private final MedicationLookupAdapter lookup = new MedicationLookupAdapter(treatment);

  private MedicationResult medication(String id, String adult, String name) {
    return new MedicationResult(id, adult, name, "Tablet", true);
  }

  @Test
  void sourcePagesAreDetachedAndMalformedPositionsAreTechnicalErrors() {
    var original = new ArrayList<>(List.of("a", "b"));
    when(treatment.findMedicationIdsAfter("adult", "", 2)).thenReturn(original);
    var page = source.findMedicationIdsAfter("adult", "", 2);
    original.clear();
    assertEquals(List.of("a", "b"), page);
    assertThrows(UnsupportedOperationException.class, () -> page.add("c"));
    for (var invalid :
        List.of(
            List.of("a", "a"),
            List.of("b", "a"),
            List.of(" "),
            List.of("a", "b", "c"),
            Arrays.asList("a", null))) {
      when(treatment.findMedicationIdsAfter("adult", "", 2)).thenReturn(invalid);
      assertThrows(
          InvalidMonitoringSourceException.class,
          () -> source.findMedicationIdsAfter("adult", "", 2));
    }
    when(treatment.findMedicationIdsAfter("adult", "b", 2)).thenReturn(List.of("a"));
    assertThrows(
        InvalidMonitoringSourceException.class,
        () -> source.findMedicationIdsAfter("adult", "b", 2));
  }

  @Test
  void sourceOwnershipConflictPreventsInventoryRead() {
    when(treatment.reserveMedication("medication"))
        .thenReturn(Optional.of(medication("medication", "foreign", "Losartan")));
    assertThrows(
        InvalidMonitoringSourceException.class, () -> source.reserve("adult", "medication"));
    verifyNoInteractions(inventory);
  }

  @Test
  void existingMedicationWithoutStockIsValidButInvalidStockIsNotAbsence() {
    when(treatment.reserveMedication("medication"))
        .thenReturn(Optional.of(medication("medication", "adult", "Losartan")));
    when(inventory.reserveStock("medication")).thenReturn(Optional.empty());
    assertTrue(source.reserve("adult", "medication").orElseThrow().stock().isEmpty());
    when(inventory.reserveStock("medication"))
        .thenReturn(
            Optional.of(
                new InventoryStockSnapshot("inventory", "medication", -1, 5, 1, Instant.EPOCH)));
    assertThrows(
        InvalidMonitoringSourceException.class, () -> source.reserve("adult", "medication"));
  }

  @Test
  void medicationLookupChecksSourceIdentityOwnerAndName() {
    when(treatment.findMedication("medication")).thenReturn(Optional.empty());
    assertTrue(lookup.findMedication("medication").isEmpty());
    for (var invalid :
        List.of(
            medication("other", "adult", "Losartan"),
            medication("medication", " ", "Losartan"),
            medication("medication", "adult", " "))) {
      when(treatment.findMedication("medication")).thenReturn(Optional.of(invalid));
      assertThrows(
          InvalidMonitoringSourceException.class, () -> lookup.findMedication("medication"));
    }
  }

  @Test
  void providerOutageRetainsItsTechnicalFailure() {
    var failure = new IllegalStateException("controlled source outage");
    when(treatment.findMedicationIdsAfter("adult", "", 1)).thenThrow(failure);
    assertSame(
        failure,
        assertThrows(
            IllegalStateException.class, () -> source.findMedicationIdsAfter("adult", "", 1)));
  }
}
