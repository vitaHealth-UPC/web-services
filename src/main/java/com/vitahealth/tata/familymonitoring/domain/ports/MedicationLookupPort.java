package com.vitahealth.tata.familymonitoring.domain.ports;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.MedicationInfo;
import java.util.Optional;

/** Port to Treatment Management: who owns a medication and how it is called. */
public interface MedicationLookupPort {

  /** Returns source medication ownership, or absence when the medication does not exist. */
  Optional<MedicationInfo> findMedication(String medicationId);
}
