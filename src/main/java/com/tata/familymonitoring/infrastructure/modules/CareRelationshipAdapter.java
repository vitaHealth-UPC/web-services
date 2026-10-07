package com.tata.familymonitoring.infrastructure.modules;

import com.tata.carelink.application.queryservices.CareLinkQueryService;
import com.tata.familymonitoring.domain.ports.ICareRelationshipPort;
import org.springframework.stereotype.Component;

@Component
public class CareRelationshipAdapter implements ICareRelationshipPort {
    private final CareLinkQueryService queries;
    public CareRelationshipAdapter(CareLinkQueryService queries) { this.queries = queries; }
    @Override public boolean isAuthorized(String caregiverId, String olderAdultId) {
        return queries.isAuthorized(caregiverId, olderAdultId);
    }
}
