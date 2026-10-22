package com.vitahealth.tata.inventoryreplenishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.inventoryreplenishment.application.exceptions.InvalidInventorySourceException;
import com.vitahealth.tata.inventoryreplenishment.application.internal.outboundservices.MedicationCatalog.Availability;
import com.vitahealth.tata.inventoryreplenishment.infrastructure.acl.TreatmentMedicationCatalog;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.interfaces.acl.TreatmentContextFacade;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Verifies owning-context absence and malformed evidence remain distinct at the ACL. */
class TreatmentMedicationCatalogTest {
  private final TreatmentContextFacade source = mock(TreatmentContextFacade.class);
  private final TreatmentMedicationCatalog catalog = new TreatmentMedicationCatalog(source);

  @Test
  void preservesActualAbsenceAndCurrentAvailability() {
    when(source.findMedication("medication")).thenReturn(Optional.empty());
    assertEquals(Availability.MISSING, catalog.availability("medication"));
    when(source.findMedication("medication"))
        .thenReturn(
            Optional.of(new MedicationResult("medication", "adult", "Losartan", "Tablet", true)));
    assertEquals(Availability.ACTIVE, catalog.availability("medication"));
    when(source.findMedication("medication"))
        .thenReturn(
            Optional.of(new MedicationResult("medication", "adult", "Losartan", "Tablet", false)));
    assertEquals(Availability.INACTIVE, catalog.availability("medication"));
  }

  @Test
  void rejectsWrongIdentityOrMissingOwnerForReadsAndRegistrationReservations() {
    for (var invalid :
        new MedicationResult[] {
          new MedicationResult("other", "adult", "Losartan", "Tablet", true),
          new MedicationResult("medication", " ", "Losartan", "Tablet", true)
        }) {
      when(source.findMedication("medication")).thenReturn(Optional.of(invalid));
      when(source.reserveMedication("medication")).thenReturn(Optional.of(invalid));
      assertThrows(InvalidInventorySourceException.class, () -> catalog.availability("medication"));
      assertThrows(
          InvalidInventorySourceException.class,
          () -> catalog.reserveForInventoryRegistration("medication"));
    }
  }

  @Test
  void unknownCoverageStaysUnknownButNonpositiveProviderEvidenceFails() {
    when(source.scheduledDailyUnits("medication")).thenReturn(Optional.empty());
    assertNull(catalog.dailyConsumptionUnits("medication"));
    when(source.scheduledDailyUnits("medication")).thenReturn(Optional.of(2));
    assertEquals(2, catalog.dailyConsumptionUnits("medication"));
    for (int invalid : new int[] {0, -1}) {
      when(source.scheduledDailyUnits("medication")).thenReturn(Optional.of(invalid));
      assertThrows(
          InvalidInventorySourceException.class, () -> catalog.dailyConsumptionUnits("medication"));
    }
  }

  @Test
  void owningContextTechnicalFailureIsNotConvertedToMissingData() {
    var failure = new IllegalStateException("controlled provider failure");
    when(source.findMedication("medication")).thenThrow(failure);
    assertSame(
        failure,
        assertThrows(IllegalStateException.class, () -> catalog.availability("medication")));
  }
}
