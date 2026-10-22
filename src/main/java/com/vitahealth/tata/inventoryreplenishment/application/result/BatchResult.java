package com.vitahealth.tata.inventoryreplenishment.application.result;

import java.time.Instant;

/** Immutable receipt evidence returned by inventory application services. */
public record BatchResult(String id, int quantity, Instant registeredAt, String lot) {
  /** Builds receipt evidence without an optional lot reference. */
  public BatchResult(String id, int quantity, Instant registeredAt) {
    this(id, quantity, registeredAt, null);
  }
}
