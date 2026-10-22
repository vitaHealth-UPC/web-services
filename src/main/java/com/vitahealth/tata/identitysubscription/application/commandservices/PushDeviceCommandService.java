package com.vitahealth.tata.identitysubscription.application.commandservices;

import com.vitahealth.tata.identitysubscription.application.result.PushDeviceResult;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterPushDeviceCommand;
import com.vitahealth.tata.identitysubscription.domain.model.commands.UnregisterPushDeviceCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;

/** Registers and removes device addresses under the caller's verified session ownership. */
public interface PushDeviceCommandService {
  /** Registers or rotates an installation without accepting a caller-selected account identity. */
  Result<PushDeviceResult, ApplicationError> register(
      RegisterPushDeviceCommand command, String credential);

  /** Removes only the caller's own installation; missing registrations are harmless retries. */
  Result<Boolean, ApplicationError> unregister(
      UnregisterPushDeviceCommand command, String credential);
}
