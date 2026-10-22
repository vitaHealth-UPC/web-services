package com.vitahealth.tata.treatmentmanagement.application.internal;

import com.vitahealth.tata.treatmentmanagement.application.internal.authorization.TreatmentCareLinkGuard;
import com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.CareLinkVerificationPort;
import com.vitahealth.tata.treatmentmanagement.application.internal.queryservices.*;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;

/** Wires real query handlers around explicit test repositories and authorization. */
public final class TreatmentQueryServiceFixture {
  private TreatmentQueryServiceFixture() {}

  /** Creates the application facade without adding production fallback constructors. */
  public static TreatmentQueryServiceImpl create(
      MedicationRepository medications,
      TreatmentRepository treatments,
      CareLinkVerificationPort links) {
    var access = new TreatmentCareLinkGuard(links);
    return new TreatmentQueryServiceImpl(
        new GetMedicationQueryHandler(medications, access),
        new GetTreatmentQueryHandler(treatments, access),
        new ListMedicationsQueryHandler(medications, access),
        new ListTreatmentsQueryHandler(treatments, access));
  }
}
