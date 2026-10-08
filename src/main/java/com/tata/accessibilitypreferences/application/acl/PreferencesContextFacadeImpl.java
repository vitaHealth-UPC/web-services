package com.tata.accessibilitypreferences.application.acl;

import com.tata.accessibilitypreferences.application.models.NotificationPreferencesResult;
import com.tata.accessibilitypreferences.interfaces.acl.PreferencesContextFacade;
import com.tata.accessibilitypreferences.application.queryservices.UserPreferencesQueryService;
import org.springframework.stereotype.Service;

@Service
public class PreferencesContextFacadeImpl implements PreferencesContextFacade {
    private final UserPreferencesQueryService queries;
    public PreferencesContextFacadeImpl(UserPreferencesQueryService queries) { this.queries = queries; }
    @Override public NotificationPreferencesResult getNotificationPreferences(String userId) { return queries.getNotificationPreferences(userId); }
    @Override public boolean isVoiceConfirmationEnabled(String userId) { return queries.isVoiceConfirmationEnabled(userId); }
}
