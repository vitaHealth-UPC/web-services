package com.vitahealth.tata.identitysubscription.application.commandservices;

import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.shared.application.result.*;

public interface SocialSessionCommandService {
  Result<SessionResult, ApplicationError> authenticate(String firebaseIdToken);
  Result<Boolean, ApplicationError> link(String firebaseIdToken, String accountId);
}
