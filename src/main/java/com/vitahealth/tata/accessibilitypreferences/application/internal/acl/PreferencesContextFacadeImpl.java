package com.vitahealth.tata.accessibilitypreferences.application.internal.acl;

import com.vitahealth.tata.accessibilitypreferences.application.queryservices.UserPreferencesQueryService;
import com.vitahealth.tata.accessibilitypreferences.application.result.NotificationPreferencesResult;
import com.vitahealth.tata.accessibilitypreferences.interfaces.acl.PreferencesContextFacade;
import org.springframework.stereotype.Service;

/** Delegates cross-context reads to the preference application's query contracts. */
@Service
public class PreferencesContextFacadeImpl implements PreferencesContextFacade {
  private final UserPreferencesQueryService queries;

  /** Injects the minimal query surface exposed to other contexts. */
  public PreferencesContextFacadeImpl(UserPreferencesQueryService queries) {
    this.queries = queries;
  }

  /** Returns notification choices without applying notification delivery or timing rules. */
  @Override
  public NotificationPreferencesResult getNotificationPreferences(String userId) {
    return queries.getNotificationPreferences(userId);
  }

  /** Reads the same voice setting used by the public preference query. */
  @Override
  public boolean isVoiceConfirmationEnabled(String userId) {
    return queries.isVoiceConfirmationEnabled(userId);
  }
}
