package com.vitahealth.tata.intakeexecution.application.result;

/** Outcome of the external speech-recognition boundary, before clinical phrase validation. */
public enum RecognitionStatus {
  RECOGNIZED,
  UNRECOGNIZED,
  UNAVAILABLE
}
