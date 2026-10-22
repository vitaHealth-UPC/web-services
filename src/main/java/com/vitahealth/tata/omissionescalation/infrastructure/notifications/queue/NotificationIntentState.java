package com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue;

/** Technical durable fanout state, independent of clinical omission and alert state. */
public enum NotificationIntentState {
  /** Recipient fanout remains eligible for reservation. */
  READY,
  /** All selected device delivery records have been materialized. */
  FANOUT_COMPLETE,
  /** At least one acceptance and no pending delivery were reflected in the source case. */
  COMPLETED,
  /** Source work was cancelled before aggregate delivery completion. */
  CANCELLED
}
