package com.tata.inventoryreplenishment.application.internal;

public final class InventoryApplicationException extends RuntimeException {
    public enum Code {
        MEDICATION_NOT_FOUND,
        MEDICATION_INACTIVE,
        INVENTORY_NOT_FOUND,
        INVENTORY_ALREADY_EXISTS,
        INVALID_QUANTITY,
        INSUFFICIENT_STOCK
    }

    private final Code code;

    public InventoryApplicationException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() { return code; }
}
