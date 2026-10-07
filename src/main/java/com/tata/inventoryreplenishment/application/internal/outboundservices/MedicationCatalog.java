package com.tata.inventoryreplenishment.application.internal.outboundservices;

public interface MedicationCatalog {
    enum Availability { MISSING, ACTIVE, INACTIVE }
    Availability availability(String medicationId);
}