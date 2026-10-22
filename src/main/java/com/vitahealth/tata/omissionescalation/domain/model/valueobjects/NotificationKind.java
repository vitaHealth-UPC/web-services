package com.vitahealth.tata.omissionescalation.domain.model.valueobjects;

/** Distinguishes the adult's pending-intake reminder from its caregivers' omission alert. */
public enum NotificationKind {
  /** Reminder while the source intake is still pending. */
  REINFORCED_REMINDER,
  /** Caregiver alert after a definitive omission. */
  CAREGIVER_ALERT
}
