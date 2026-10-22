package com.vitahealth.tata.omissionescalation.domain.model.valueobjects;

/** Clinical lifecycle of one unconfirmed scheduled intake tracked by the omission context. */
public enum OmissionCaseStatus {
  /** Confirmation remains possible before definitive omission is registered. */
  PENDING,
  /** Intake Execution recorded a valid confirmation for the pending case. */
  RESOLVED,
  /** The clinical grace interval ended without confirmation. */
  OMITTED,
  /** The unresolved omission reached an escalation level. */
  ESCALATED,
  /** Attention to the definitive omission ended without rewriting its clinical history. */
  CLOSED
}
