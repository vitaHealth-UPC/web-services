package com.vitahealth.tata.identitysubscription.interfaces.rest;

import com.vitahealth.tata.identitysubscription.application.commandservices.PinCommandService;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.RegisterPinResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.IdentityErrorResponseAssembler;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.RegisterPinCommandFromResourceAssembler;
import com.vitahealth.tata.shared.interfaces.rest.transform.ResponseEntityAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes adult PIN operations through explicit application result contracts. */
@RestController
@RequestMapping("/api/v1/pin-credentials")
public class PinCredentialsController {
  private final IdentityErrorResponseAssembler errors;
  private final PinCommandService service;

  /** Injects the PIN command contract without persistence or hashing details. */
  public PinCredentialsController(
      PinCommandService service, IdentityErrorResponseAssembler errors) {
    this.errors = errors;
    this.service = service;
  }

  /** Translates validated HTTP input and maps expected application failures. */
  @PostMapping
  public ResponseEntity<?> register(@Valid @RequestBody RegisterPinResource resource) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        errors.localize(
            service.register(RegisterPinCommandFromResourceAssembler.toCommand(resource))),
        value -> null,
        HttpStatus.CREATED,
        IdentityErrorResponseAssembler::status);
  }
}
