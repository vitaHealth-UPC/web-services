package com.tata.carelink.interfaces.acl;

import com.tata.carelink.application.models.CareLinkResult;
import com.tata.carelink.application.models.OlderAdultProfileResult;

public interface CareLinkContextFacade {
    CareLinkResult getById(String careLinkId);
    OlderAdultProfileResult getOlderAdult(String olderAdultId);
    boolean isAuthorized(String caregiverId, String olderAdultId);
    java.util.List<CareLinkResult> getConfirmedByCaregiver(String caregiverId);
    java.util.Optional<OlderAdultProfileResult> findOlderAdult(String olderAdultId);
}
