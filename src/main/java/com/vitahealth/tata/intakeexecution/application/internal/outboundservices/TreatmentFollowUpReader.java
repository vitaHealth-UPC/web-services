package com.vitahealth.tata.intakeexecution.application.internal.outboundservices;

import java.util.Optional;

/** Reads an optional treatment-specific follow-up delay without exposing another aggregate. */
public interface TreatmentFollowUpReader {
  Optional<Integer> findDelayMinutes(String treatmentId, String olderAdultId);
}
