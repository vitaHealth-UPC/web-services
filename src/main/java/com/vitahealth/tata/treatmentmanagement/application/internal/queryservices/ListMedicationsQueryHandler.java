package com.vitahealth.tata.treatmentmanagement.application.internal.queryservices;

import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentMapper;
import com.vitahealth.tata.treatmentmanagement.application.internal.authorization.TreatmentCareLinkGuard;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.ListMedicationsQuery;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns the authorized listMedications read and its projection within a read-only transaction. */
@Service
@Transactional(readOnly = true)
public class ListMedicationsQueryHandler {
  private final MedicationRepository medications;
  private final TreatmentCareLinkGuard access;

  /** Injects context-owned persistence and current consent authorization. */
  public ListMedicationsQueryHandler(
      MedicationRepository medications, TreatmentCareLinkGuard access) {
    this.medications = medications;
    this.access = access;
  }

  /** Returns the requested projection after enforcing caregiver access. */
  public List<MedicationResult> handle(ListMedicationsQuery query) {
    access.requireAuthorized(query.caregiverId(), query.olderAdultId());
    return medications.findByOlderAdultId(query.olderAdultId()).stream()
        .map(TreatmentMapper::toResult)
        .toList();
  }
}
