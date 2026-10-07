package com.tata.familymonitoring.application.internal;

import com.tata.familymonitoring.domain.ports.ICareRelationshipPort;
import org.springframework.stereotype.Service;

@Service
public class RequireCareRelationship implements com.tata.familymonitoring.application.queryservices.CareRelationshipQueryService {
    public static final class AccessDenied extends RuntimeException {
        public AccessDenied() { super("an active confirmed care relationship is required"); }
    }
    private final ICareRelationshipPort relationships;
    public RequireCareRelationship(ICareRelationshipPort relationships) { this.relationships = relationships; }
    public void check(String caregiverId, String olderAdultId) {
        if (caregiverId == null || caregiverId.isBlank() || olderAdultId == null || olderAdultId.isBlank())
            throw new IllegalArgumentException("caregiverId and olderAdultId are required");
        if (!relationships.isAuthorized(caregiverId.trim(), olderAdultId.trim())) throw new AccessDenied();
    }
}
