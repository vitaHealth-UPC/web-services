package com.vitahealth.tata.inventoryreplenishment.application.exceptions;

/** Reports inconsistent owning-context evidence independently from a valid inventory request. */
public final class InvalidInventorySourceException extends RuntimeException {
  /** Retains the technical cause without exposing its contents through the public API. */
  public InvalidInventorySourceException(RuntimeException cause) {
    super("Inventory source evidence is inconsistent", cause);
  }
}
