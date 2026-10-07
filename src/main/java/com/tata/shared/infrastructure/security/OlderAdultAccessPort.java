package com.tata.shared.infrastructure.security;

@FunctionalInterface
public interface OlderAdultAccessPort {
    boolean canAccess(String subjectId, String olderAdultId);
}
