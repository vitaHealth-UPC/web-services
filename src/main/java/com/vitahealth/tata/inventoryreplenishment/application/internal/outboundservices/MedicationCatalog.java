package com.vitahealth.tata.inventoryreplenishment.application.internal.outboundservices;

/** Queries public Treatment evidence without depending on its persistence model. */
public interface MedicationCatalog {
  /** Distinguishes missing medication from active and inactive registered medication. */
  enum Availability {
    MISSING,
    ACTIVE,
    INACTIVE
  }

  /** Reserves source registration even before inventory exists, within the caller transaction. */
  void reserveForInventoryRegistration(String medicationId);

  /** Reads medication registration and activity from the owning context. */
  Availability availability(String medicationId);

  /** Returns scheduled inventory units per day, or null when coverage cannot be determined. */
  Integer dailyConsumptionUnits(String medicationId);
}
