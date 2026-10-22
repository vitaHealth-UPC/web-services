package com.vitahealth.tata.identitysubscription.interfaces.rest;

import com.vitahealth.tata.identitysubscription.application.commandservices.PushDeviceCommandService;
import com.vitahealth.tata.identitysubscription.domain.model.commands.UnregisterPushDeviceCommand;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.RegisterPushDeviceResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.IdentityErrorResponseAssembler;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.PushDeviceResourceFromResultAssembler;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.RegisterPushDeviceCommandFromResourceAssembler;
import com.vitahealth.tata.shared.interfaces.rest.transform.ResponseEntityAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes current-session installation registration without accepting caller-selected ownership.
 */
@RestController
@RequestMapping("/api/v1/push-devices")
public class PushDevicesController {
  private final PushDeviceCommandService commands;
  private final IdentityErrorResponseAssembler errors;

  /** Injects typed commands and Identity's localized error policy. */
  public PushDevicesController(
      PushDeviceCommandService commands, IdentityErrorResponseAssembler errors) {
    this.commands = commands;
    this.errors = errors;
  }

  /** Registers or rotates a device address after the authentication filter verifies its bearer. */
  @PutMapping("/{installationId}")
  public ResponseEntity<?> register(
      @PathVariable String installationId,
      @Valid @RequestBody RegisterPushDeviceResource resource,
      @RequestHeader("Authorization") String authorization) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        errors.localize(
            commands.register(
                RegisterPushDeviceCommandFromResourceAssembler.toCommand(installationId, resource),
                authorization.substring(7))),
        PushDeviceResourceFromResultAssembler::toResource,
        HttpStatus.OK,
        IdentityErrorResponseAssembler::status);
  }

  /** Removes the caller's registration; successful retries return an empty response. */
  @DeleteMapping("/{installationId}")
  public ResponseEntity<?> unregister(
      @PathVariable String installationId, @RequestHeader("Authorization") String authorization) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        errors.localize(
            commands.unregister(
                new UnregisterPushDeviceCommand(installationId), authorization.substring(7))),
        ignored -> null,
        HttpStatus.NO_CONTENT,
        IdentityErrorResponseAssembler::status);
  }
}
