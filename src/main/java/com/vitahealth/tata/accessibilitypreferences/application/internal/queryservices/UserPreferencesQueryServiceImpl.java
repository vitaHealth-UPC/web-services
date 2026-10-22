package com.vitahealth.tata.accessibilitypreferences.application.internal.queryservices;

import com.vitahealth.tata.accessibilitypreferences.application.internal.assemblers.NotificationPreferencesResultFromResultAssembler;
import com.vitahealth.tata.accessibilitypreferences.application.queryservices.GetUserPreferencesQueryService;
import com.vitahealth.tata.accessibilitypreferences.application.queryservices.UserPreferencesQueryService;
import com.vitahealth.tata.accessibilitypreferences.application.result.NotificationPreferencesResult;
import com.vitahealth.tata.accessibilitypreferences.domain.model.queries.GetUserPreferencesQuery;
import org.springframework.stereotype.Service;

/** Adapts the single public preference read into the minimal outcomes consumed across contexts. */
@Service
public class UserPreferencesQueryServiceImpl implements UserPreferencesQueryService {
  private final GetUserPreferencesQueryService preferences;

  /** Delegates stored/default selection to its transaction-owning query contract. */
  public UserPreferencesQueryServiceImpl(GetUserPreferencesQueryService preferences) {
    this.preferences = preferences;
  }

  /** Reads notification channel choices and quiet hours without persisting defaults. */
  @Override
  public NotificationPreferencesResult getNotificationPreferences(String userId) {
    return NotificationPreferencesResultFromResultAssembler.toResult(
        preferences.handle(new GetUserPreferencesQuery(userId)));
  }

  /** Reads the current user's voice choice through the same stored/default selection. */
  @Override
  public boolean isVoiceConfirmationEnabled(String userId) {
    return preferences.handle(new GetUserPreferencesQuery(userId)).voiceConfirmationEnabled();
  }
}
