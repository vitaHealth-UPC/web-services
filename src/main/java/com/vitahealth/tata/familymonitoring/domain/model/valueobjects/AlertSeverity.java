package com.vitahealth.tata.familymonitoring.domain.model.valueobjects;

/** Operational follow-up priority derived from recorded escalation, not a medical diagnosis. */
public enum AlertSeverity {
  UNKNOWN,
  STANDARD,
  HIGH,
  CRITICAL;

  /** Maps the source context's supported levels; missing evidence is handled as UNKNOWN. */
  public static AlertSeverity fromEscalationLevel(int level) {
    return switch (level) {
      case 0 -> STANDARD;
      case 1, 2 -> HIGH;
      case 3 -> CRITICAL;
      default -> throw new IllegalArgumentException("unsupported escalation level");
    };
  }
}
