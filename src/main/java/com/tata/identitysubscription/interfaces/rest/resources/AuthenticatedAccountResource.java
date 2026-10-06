package com.tata.identitysubscription.interfaces.rest.resources;
import java.time.Instant;
public record AuthenticatedAccountResource(String accountId, String accessToken, Instant expiresAt) {}
