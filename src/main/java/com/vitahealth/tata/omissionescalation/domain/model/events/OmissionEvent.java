package com.vitahealth.tata.omissionescalation.domain.model.events;

import java.time.Instant;

/** Business facts published by Omission & Escalation with their original intake reference. */
public sealed interface OmissionEvent
    permits IntakeOmitted, CaregiverAlertGenerated, EscalationExecuted {

  /** Identifies the scheduled intake that caused this fact. */
  String intakeId();

  /** Identifies the adult whose omission case produced this fact. */
  String olderAdultId();

  /** Returns the business occurrence time, independently of notification delivery. */
  Instant occurredAt();
}
