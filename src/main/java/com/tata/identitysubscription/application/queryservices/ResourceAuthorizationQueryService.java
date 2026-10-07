package com.tata.identitysubscription.application.queryservices;
import com.tata.identitysubscription.application.models.AuthenticatedSession;
public interface ResourceAuthorizationQueryService {
 void authorizeRequest(AuthenticatedSession session, String path, String method);
 void authorizeSelector(AuthenticatedSession session, String selector, String value, String path, String method);
}
