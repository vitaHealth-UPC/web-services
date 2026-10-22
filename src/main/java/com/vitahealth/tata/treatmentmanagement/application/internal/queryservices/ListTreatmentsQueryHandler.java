package com.vitahealth.tata.treatmentmanagement.application.internal.queryservices;

import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentMapper;
import com.vitahealth.tata.treatmentmanagement.application.internal.authorization.TreatmentCareLinkGuard;
import com.vitahealth.tata.treatmentmanagement.application.result.TreatmentResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.ListTreatmentsQuery;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns the authorized listTreatments read and its projection within a read-only transaction. */
@Service
@Transactional(readOnly = true)
public class ListTreatmentsQueryHandler {
  private final TreatmentRepository treatments;
  private final TreatmentCareLinkGuard access;

  /** Injects context-owned persistence and current consent authorization. */
  public ListTreatmentsQueryHandler(TreatmentRepository treatments, TreatmentCareLinkGuard access) {
    this.treatments = treatments;
    this.access = access;
  }

  /** Returns the requested projection after enforcing caregiver access. */
  public List<TreatmentResult> handle(ListTreatmentsQuery query) {
    access.requireAuthorized(query.caregiverId(), query.olderAdultId());
    return treatments.findByOlderAdultId(query.olderAdultId()).stream()
        .map(TreatmentMapper::toResult)
        .toList();
  }
}
