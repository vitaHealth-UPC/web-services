package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import com.vitahealth.tata.identitysubscription.application.queryservices.SessionRegistrationQueryService;
import com.vitahealth.tata.identitysubscription.application.result.PushDeviceResult;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PushDeviceRegistration;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterPushDeviceCommand;
import com.vitahealth.tata.identitysubscription.domain.model.commands.UnregisterPushDeviceCommand;
import com.vitahealth.tata.identitysubscription.domain.repositories.PushDeviceRegistrationRepository;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Makes registration ownership decisions inside the same transaction as reserved device writes. */
@Service
public class PushDeviceMutationHandler {
  private final SessionRegistrationQueryService sessions;
  private final PushDeviceRegistrationRepository devices;
  private final Clock clock;

  /** Injects fresh identity evidence, reserved device storage and explicit application time. */
  public PushDeviceMutationHandler(
      SessionRegistrationQueryService sessions,
      PushDeviceRegistrationRepository devices,
      Clock clock) {
    this.sessions = sessions;
    this.devices = devices;
    this.clock = clock;
  }

  /**
   * Rotates only the current session or atomically replaces a registration whose session is
   * inactive.
   */
  @Transactional
  public Result<PushDeviceResult, ApplicationError> register(
      RegisterPushDeviceCommand command, String credential) {
    var caller = sessions.authenticateRegistration(credential);
    if (caller.isEmpty()) return failure("INVALID_CREDENTIALS", "A valid session is required");
    if (caller.orElseThrow().identity().role() == Role.LINK_SETUP)
      return failure("DEVICE_ACCESS_DENIED", "This session cannot register a device");
    final PushDeviceRegistration requested;
    try {
      requested =
          PushDeviceRegistration.register(
              command.installationId(),
              caller.orElseThrow().sessionId(),
              command.token(),
              clock.instant());
    } catch (IllegalArgumentException invalid) {
      return failure("VALIDATION_ERROR", "Device registration is invalid");
    }
    var existing = devices.findByInstallationIdForUpdate(requested.installationId());
    PushDeviceRegistration next = requested;
    if (existing.isPresent()) {
      var previous = existing.orElseThrow();
      if (!previous.sessionId().equals(requested.sessionId())) {
        if (sessions.findActiveRegistration(previous.sessionId()).isPresent())
          return failure(
              "DEVICE_REGISTRATION_CONFLICT", "The installation belongs to another active session");
        devices.deleteOwned(previous.installationId(), previous.sessionId());
        next =
            PushDeviceRegistration.register(
                requested.installationId(),
                requested.sessionId(),
                requested.token(),
                clock.instant());
      } else {
        if (previous.token().equals(requested.token()))
          return Result.success(
              new PushDeviceResult(previous.installationId(), previous.updatedAt()));
        next = previous.rotate(requested.token(), clock.instant());
      }
    }
    var saved = devices.save(next);
    return Result.success(new PushDeviceResult(saved.installationId(), saved.updatedAt()));
  }

  /**
   * Removes only exact session ownership and rejects setup credentials before accessing devices.
   */
  @Transactional
  public Result<Boolean, ApplicationError> unregister(
      UnregisterPushDeviceCommand command, String credential) {
    var caller = sessions.authenticateRegistration(credential);
    if (caller.isEmpty()) return failure("INVALID_CREDENTIALS", "A valid session is required");
    if (caller.orElseThrow().identity().role() == Role.LINK_SETUP)
      return failure("DEVICE_ACCESS_DENIED", "This session cannot unregister a device");
    try {
      if (!UUID.fromString(command.installationId()).toString().equals(command.installationId()))
        return failure("VALIDATION_ERROR", "Installation id is invalid");
    } catch (IllegalArgumentException | NullPointerException invalid) {
      return failure("VALIDATION_ERROR", "Installation id is invalid");
    }
    var existing = devices.findByInstallationIdForUpdate(command.installationId());
    if (existing.isEmpty()) return Result.success(true);
    if (!existing.orElseThrow().sessionId().equals(caller.orElseThrow().sessionId()))
      return failure("DEVICE_ACCESS_DENIED", "The installation belongs to another session");
    devices.deleteOwned(command.installationId(), caller.orElseThrow().sessionId());
    return Result.success(true);
  }

  private static <T> Result<T, ApplicationError> failure(String code, String message) {
    return Result.failure(new ApplicationError(code, message));
  }
}
