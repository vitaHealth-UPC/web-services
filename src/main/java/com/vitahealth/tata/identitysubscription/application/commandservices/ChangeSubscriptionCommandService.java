package com.vitahealth.tata.identitysubscription.application.commandservices;

import com.vitahealth.tata.identitysubscription.application.result.SubscriptionResult;
import com.vitahealth.tata.identitysubscription.domain.model.commands.ChangeSubscriptionCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;

/** Changes a plan and returns explicit validation, ownership and account-state outcomes. */
public interface ChangeSubscriptionCommandService {
  /** Applies a valid plan to the account; repeated selection preserves renewal state. */
  Result<SubscriptionResult, ApplicationError> handle(ChangeSubscriptionCommand command);
}
