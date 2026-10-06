package com.tata.carelink.application.queryservices;

import com.tata.carelink.application.models.CareLinkResult;
import com.tata.carelink.application.models.OlderAdultProfileResult;

public interface CareLinkQueryService {
    CareLinkResult getById(String careLinkId);
    OlderAdultProfileResult getOlderAdult(String olderAdultId);
    boolean isAuthorized(String caregiverId, String olderAdultId);
}
