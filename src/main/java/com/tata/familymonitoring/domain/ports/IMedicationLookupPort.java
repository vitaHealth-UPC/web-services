package com.tata.familymonitoring.domain.ports;

import java.util.Optional;

/** Port to Treatment Management: who owns a medication and how it is called. */
public interface IMedicationLookupPort {

  record MedicationInfo(String olderAdultId, String name) {
  }

  Optional<MedicationInfo> findMedication(String medicationId);
}
