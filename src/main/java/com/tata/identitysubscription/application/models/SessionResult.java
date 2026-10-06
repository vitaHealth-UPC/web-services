package com.tata.identitysubscription.application.models;

import java.time.Instant;

public record SessionResult(String accountId, String accessToken, Instant expiresAt) {}
