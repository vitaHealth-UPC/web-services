package com.vitahealth.tata.omissionescalation.domain.model.valueobjects;

/** Business alert delivery evidence; creation alone does not establish provider acceptance. */
public enum AlertStatus {
  /** The alert exists and durable delivery work can be queued. */
  GENERATED,
  /** Completed recipient delivery has actual provider acceptance evidence. */
  SENT,
  /** A historical failed delivery remains recorded until an accepted retry completes. */
  FAILED
}
