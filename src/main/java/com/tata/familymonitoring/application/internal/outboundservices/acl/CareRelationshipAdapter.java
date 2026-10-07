package com.tata.familymonitoring.application.internal.outboundservices.acl;

import com.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.tata.familymonitoring.domain.ports.ICareRelationshipPort;
import org.springframework.stereotype.Component;

@Component
public class CareRelationshipAdapter implements ICareRelationshipPort {
    private final CareLinkContextFacade queries;
    public CareRelationshipAdapter(CareLinkContextFacade queries) { this.queries = queries; }
    @Override public boolean isAuthorized(String caregiverId, String olderAdultId) {
        return queries.isAuthorized(caregiverId, olderAdultId);
    }
}
