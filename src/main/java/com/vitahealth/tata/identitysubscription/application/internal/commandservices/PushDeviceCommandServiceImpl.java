package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import com.vitahealth.tata.identitysubscription.application.commandservices.PushDeviceCommandService;
import com.vitahealth.tata.identitysubscription.application.result.PushDeviceResult;
import com.vitahealth.tata.identitysubscription.domain.exceptions.DuplicatePushDeviceRegistration;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterPushDeviceCommand;
import com.vitahealth.tata.identitysubscription.domain.model.commands.UnregisterPushDeviceCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Suspends caller transactions and translates a unique-write race after mutation rollback. */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class PushDeviceCommandServiceImpl implements PushDeviceCommandService {
  private final PushDeviceMutationHandler mutations;

  /** Injects the independently proxied transaction boundary. */
  public PushDeviceCommandServiceImpl(PushDeviceMutationHandler mutations) {
    this.mutations = mutations;
  }

  /** Returns a stable conflict without reusing a failed persistence transaction. */
  @Override
  public Result<PushDeviceResult, ApplicationError> register(
      RegisterPushDeviceCommand command, String credential) {
    try {
      return mutations.register(command, credential);
    } catch (DuplicatePushDeviceRegistration conflict) {
      return Result.failure(
          new ApplicationError(
              "DEVICE_REGISTRATION_CONFLICT", "The device address is already registered"));
    }
  }

  /** Delegates owned removal; unrelated persistence failures remain technical failures. */
  @Override
  public Result<Boolean, ApplicationError> unregister(
      UnregisterPushDeviceCommand command, String credential) {
    return mutations.unregister(command, credential);
  }
}
