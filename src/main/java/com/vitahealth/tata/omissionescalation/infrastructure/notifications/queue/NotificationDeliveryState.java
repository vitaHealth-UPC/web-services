package com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue;

/** Technical delivery outcome for one installation, independent of clinical confirmation. */
public enum NotificationDeliveryState {
  /** Eligible for provider dispatch or retry. */
  READY,
  /** The provider returned an acceptance receipt. */
  ACCEPTED,
  /** Authorization or source eligibility no longer permits delivery. */
  CANCELLED
}
