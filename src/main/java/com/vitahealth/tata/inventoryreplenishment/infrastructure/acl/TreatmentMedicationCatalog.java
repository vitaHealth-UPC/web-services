package com.vitahealth.tata.inventoryreplenishment.infrastructure.acl;

import com.vitahealth.tata.inventoryreplenishment.application.exceptions.InvalidInventorySourceException;
import com.vitahealth.tata.inventoryreplenishment.application.internal.outboundservices.MedicationCatalog;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.interfaces.acl.TreatmentContextFacade;
import org.springframework.stereotype.Component;

/** Translates public Treatment queries into inventory availability and coverage evidence. */
@Component
public class TreatmentMedicationCatalog implements MedicationCatalog {
  private final TreatmentContextFacade treatments;

  /** Injects the owning context public facade. */
  public TreatmentMedicationCatalog(TreatmentContextFacade treatments) {
    this.treatments = treatments;
  }

  /** Serializes first inventory creation against other callers reserving the source medication. */
  @Override
  public void reserveForInventoryRegistration(String medicationId) {
    treatments
        .reserveMedication(medicationId)
        .ifPresent(medication -> validateIdentity(medicationId, medication));
  }

  /** Preserves absent schedule coverage instead of inventing a daily consumption rate. */
  @Override
  public Integer dailyConsumptionUnits(String medicationId) {
    var units = treatments.scheduledDailyUnits(medicationId);
    if (units.isPresent() && units.get() <= 0)
      throw new InvalidInventorySourceException(
          new IllegalStateException("scheduled source units must be positive"));
    return units.orElse(null);
  }

  /** Distinguishes missing and inactive medication using actual Treatment evidence. */
  @Override
  public Availability availability(String medicationId) {
    return treatments
        .findMedication(medicationId)
        .map(
            medication -> {
              validateIdentity(medicationId, medication);
              return medication.active() ? Availability.ACTIVE : Availability.INACTIVE;
            })
        .orElse(Availability.MISSING);
  }

  private static void validateIdentity(String requestedId, MedicationResult medication) {
    if (!requestedId.equals(medication.id())
        || medication.olderAdultId() == null
        || medication.olderAdultId().isBlank())
      throw new InvalidInventorySourceException(
          new IllegalStateException("medication source identity is inconsistent"));
  }
}
