package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.domain.model.commands.ChangeSubscriptionCommand;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.ChangeSubscriptionResource;

/** Translates a subscription change request into its command. */
public final class ChangeSubscriptionCommandFromResourceAssembler {
  private ChangeSubscriptionCommandFromResourceAssembler() {}

  /** Combines the authorized route account with the requested plan identifier. */
  public static ChangeSubscriptionCommand toCommand(
      String accountId, ChangeSubscriptionResource resource) {
    return new ChangeSubscriptionCommand(accountId, resource.planCode());
  }
}
