package com.vitahealth.tata.accessibilitypreferences.application.commandservices;

import com.vitahealth.tata.accessibilitypreferences.application.result.UserPreferencesResult;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationPreferencesCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;

/** Applies the combined quiet-hours and channel replacement for one user. */
public interface NotificationPreferencesCommandService {
  /**
   * Preserves policy, contacts and accessibility choices; failures roll back the complete change.
   */
  Result<UserPreferencesResult, ApplicationError> update(
      UpdateNotificationPreferencesCommand command);
}
