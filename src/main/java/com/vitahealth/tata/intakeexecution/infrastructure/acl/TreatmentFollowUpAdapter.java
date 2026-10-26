package com.vitahealth.tata.intakeexecution.infrastructure.acl;

import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.TreatmentFollowUpReader;
import com.vitahealth.tata.treatmentmanagement.interfaces.acl.TreatmentContextFacade;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Resolves trusted schedule evidence and checks the intake owner before applying a delay. */
@Component
public class TreatmentFollowUpAdapter implements TreatmentFollowUpReader {
  private final TreatmentContextFacade treatments;

  public TreatmentFollowUpAdapter(TreatmentContextFacade treatments) {
    this.treatments = treatments;
  }

  @Override
  public Optional<Integer> findDelayMinutes(String treatmentId, String olderAdultId) {
    var source = treatments.findTreatment(treatmentId);
    // Historical intakes retain their snapshot even when the source is no longer available.
    if (source.isEmpty()) return Optional.empty();
    var treatment = source.orElseThrow();
    if (!olderAdultId.equals(treatment.olderAdultId()))
      throw new IllegalStateException("source treatment owner is inconsistent");
    return Optional.ofNullable(treatment.followUpDelayMinutes());
  }
}
