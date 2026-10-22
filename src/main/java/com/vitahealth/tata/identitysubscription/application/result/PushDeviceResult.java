package com.vitahealth.tata.identitysubscription.application.result;

import java.time.Instant;

/**
 * Public registration confirmation without device address, session reference or credential digest.
 */
public record PushDeviceResult(String installationId, Instant updatedAt) {}
