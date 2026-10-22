package com.vitahealth.tata.identitysubscription.domain.model.commands;

/** Requests removal of an installation owned by the caller's current session. */
public record UnregisterPushDeviceCommand(String installationId) {}
