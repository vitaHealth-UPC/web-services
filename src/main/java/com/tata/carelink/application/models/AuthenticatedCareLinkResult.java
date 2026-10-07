package com.tata.carelink.application.models;
import java.time.Instant;
public record AuthenticatedCareLinkResult(CareLinkResult link, String accessToken, Instant expiresAt) {}
