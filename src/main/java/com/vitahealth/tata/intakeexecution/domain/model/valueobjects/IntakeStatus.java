package com.vitahealth.tata.intakeexecution.domain.model.valueobjects;

/** Distinguishes pending evidence from confirmed, late and definitively omitted outcomes. */
public enum IntakeStatus {
  PENDING,
  CONFIRMED,
  LATE,
  OMITTED
}
