package com.tata.familymonitoring.infrastructure.modules;

import com.tata.familymonitoring.domain.ports.IMedicationLookupPort;
import com.tata.treatmentmanagement.application.queryservices.TreatmentQueryService;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Resolves a medication through the public contract of Treatment Management. */
@Component
public class MedicationLookupAdapter implements IMedicationLookupPort {
  private final TreatmentQueryService treatments;

  public MedicationLookupAdapter(TreatmentQueryService treatments) {
    this.treatments = treatments;
  }

  @Override
  public Optional<MedicationInfo> findMedication(String medicationId) {
    return treatments.findMedication(medicationId)
        .map(medication -> new MedicationInfo(medication.olderAdultId(), medication.name()));
  }
}
