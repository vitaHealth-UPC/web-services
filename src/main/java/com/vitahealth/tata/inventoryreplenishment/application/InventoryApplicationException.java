package com.vitahealth.tata.inventoryreplenishment.application;

/** Public application failure contract; adapters translate its codes to transport responses. */
public final class InventoryApplicationException extends RuntimeException {
  /** Stable expected business failure categories translated by transport adapters. */
  public enum Code {
    MEDICATION_NOT_FOUND,
    MEDICATION_INACTIVE,
    INVENTORY_NOT_FOUND,
    INVENTORY_ALREADY_EXISTS,
    INVALID_QUANTITY,
    INSUFFICIENT_STOCK
  }

  private final Code code;

  /** Retains a business category and internal diagnostic message. */
  public InventoryApplicationException(Code code, String message) {
    super(message);
    this.code = code;
  }

  /** Returns the stable application category used for status and localized message selection. */
  public Code code() {
    return code;
  }
}
