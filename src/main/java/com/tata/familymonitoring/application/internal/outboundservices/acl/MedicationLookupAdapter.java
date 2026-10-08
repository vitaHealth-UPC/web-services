package com.tata.familymonitoring.application.internal.outboundservices.acl;

import com.tata.familymonitoring.domain.ports.IMedicationLookupPort;
import com.tata.treatmentmanagement.interfaces.acl.TreatmentContextFacade;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Resolves a medication through the public contract of Treatment Management. */
@Component
public class MedicationLookupAdapter implements IMedicationLookupPort {
  private final TreatmentContextFacade treatments;

  public MedicationLookupAdapter(TreatmentContextFacade treatments) {
    this.treatments = treatments;
  }

  @Override
  public Optional<MedicationInfo> findMedication(String medicationId) {
    return treatments.findMedication(medicationId)
        .map(medication -> new MedicationInfo(medication.olderAdultId(), medication.name()));
  }
}
