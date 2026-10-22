package com.vitahealth.tata.treatmentmanagement.application.internal;

import com.vitahealth.tata.treatmentmanagement.application.internal.authorization.TreatmentCareLinkGuard;
import com.vitahealth.tata.treatmentmanagement.application.internal.commandservices.*;
import com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.CareLinkVerificationPort;
import com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.events.TreatmentScheduleEventPublisher;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.*;
import java.time.Clock;

/** Wires real application handlers around test persistence and explicit event/time dependencies. */
public final class TreatmentCommandServiceFixture {
  private TreatmentCommandServiceFixture() {}

  /** Creates the application facade without installing production fallback dependencies. */
  public static TreatmentCommandServiceImpl create(
      MedicationRepository medications,
      TreatmentRepository treatments,
      CareLinkVerificationPort links,
      TreatmentScheduleEventPublisher publisher,
      Clock clock) {
    var access = new TreatmentCareLinkGuard(links);
    var schedules = new TreatmentSchedulePublicationService(medications, publisher);
    var lookup = new TreatmentCommandLookupService(medications, treatments);
    return new TreatmentCommandServiceImpl(
        new RegisterMedicationCommandHandler(medications, access, clock),
        new CreateTreatmentCommandHandler(treatments, access, clock),
        new UpdateMedicationCommandHandler(medications, treatments, access, schedules),
        new DeactivateMedicationCommandHandler(medications, treatments, access, schedules),
        new ConfigureTreatmentCommandHandler(lookup, treatments, access, schedules),
        new ActivateTreatmentCommandHandler(lookup, treatments, access, schedules),
        new PauseTreatmentCommandHandler(lookup, treatments, access, schedules),
        new ResumeTreatmentCommandHandler(lookup, treatments, access, schedules));
  }
}
