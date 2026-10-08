package com.tata.carelink.application.acl;

import com.tata.carelink.application.models.CareLinkResult;
import com.tata.carelink.application.models.OlderAdultProfileResult;
import com.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.tata.carelink.application.queryservices.CareLinkQueryService;
import org.springframework.stereotype.Service;

@Service
public class CareLinkContextFacadeImpl implements CareLinkContextFacade {
    private final CareLinkQueryService queries;
    public CareLinkContextFacadeImpl(CareLinkQueryService queries) { this.queries = queries; }
    @Override public CareLinkResult getById(String careLinkId) { return queries.getById(careLinkId); }
    @Override public OlderAdultProfileResult getOlderAdult(String olderAdultId) { return queries.getOlderAdult(olderAdultId); }
    @Override public boolean isAuthorized(String caregiverId, String olderAdultId) { return queries.isAuthorized(caregiverId, olderAdultId); }
    @Override public java.util.List<CareLinkResult> getConfirmedByCaregiver(String caregiverId) { return queries.getConfirmedByCaregiver(caregiverId); }
    @Override public java.util.Optional<OlderAdultProfileResult> findOlderAdult(String id) {
        try { return java.util.Optional.of(queries.getOlderAdult(id)); }
        catch (com.tata.carelink.application.CareLinkApplicationException exception) {
            if (exception.code() == com.tata.carelink.application.CareLinkApplicationException.Code.OLDER_ADULT_NOT_FOUND)
                return java.util.Optional.empty();
            throw exception;
        }
    }

}
