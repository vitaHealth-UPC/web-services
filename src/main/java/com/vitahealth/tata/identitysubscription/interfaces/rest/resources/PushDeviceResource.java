package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import java.time.Instant;

/**
 * Public confirmation of a device registration without returning its address or owner credential.
 */
public record PushDeviceResource(String installationId, Instant updatedAt) {}
