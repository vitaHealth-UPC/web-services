package com.vitahealth.tata.familymonitoring.infrastructure.acl;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.familymonitoring.application.exceptions.InvalidMonitoringSourceException;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.MedicationInfo;
import com.vitahealth.tata.familymonitoring.domain.ports.MedicationLookupPort;
import com.vitahealth.tata.treatmentmanagement.interfaces.acl.TreatmentContextFacade;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Resolves a medication through the public contract of Treatment Management. */
@Component
public class MedicationLookupAdapter implements MedicationLookupPort {
  private final TreatmentContextFacade treatments;

  /** Injects the public Treatment query boundary. */
  public MedicationLookupAdapter(TreatmentContextFacade treatments) {
    this.treatments = treatments;
  }

  /** Checks the returned source reference before exposing owner and name to projection handlers. */
  @Override
  public Optional<MedicationInfo> findMedication(String medicationId) {
    return treatments
        .findMedication(medicationId)
        .map(
            medication -> {
              try {
                if (!medicationId.equals(medication.id()))
                  throw new IllegalArgumentException("source medication identity conflicts");
                return new MedicationInfo(
                    requireText(medication.olderAdultId(), "olderAdultId"),
                    requireText(medication.name(), "medicationName"));
              } catch (IllegalArgumentException | NullPointerException invalid) {
                throw new InvalidMonitoringSourceException(invalid);
              }
            });
  }
}
