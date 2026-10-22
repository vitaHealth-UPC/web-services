package com.vitahealth.tata.accessibilitypreferences.application.commandservices;

import com.vitahealth.tata.accessibilitypreferences.application.result.UserPreferencesResult;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateReadingAssistanceCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;

/** Public application contract for one owner-scoped preference replacement. */
public interface UpdateReadingAssistanceCommandService {
  /** Returns an immutable outcome after the mutation, preserving unrelated preferences. */
  Result<UserPreferencesResult, ApplicationError> handle(UpdateReadingAssistanceCommand command);
}
