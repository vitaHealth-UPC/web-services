package com.tata.identitysubscription.interfaces.rest.resources;

import java.time.Instant;

public record PinSessionResource(String olderAdultId, String accessToken, Instant expiresAt) {}
