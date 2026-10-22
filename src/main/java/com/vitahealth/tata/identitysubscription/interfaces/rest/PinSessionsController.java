package com.vitahealth.tata.identitysubscription.interfaces.rest;

import com.vitahealth.tata.identitysubscription.application.commandservices.PinCommandService;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.PinSignInResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.AuthenticateWithPinCommandFromResourceAssembler;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.IdentityErrorResponseAssembler;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.PinSessionResourceFromResultAssembler;
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
@RequestMapping("/api/v1/pin-sessions")
public class PinSessionsController {
  private final IdentityErrorResponseAssembler errors;
  private final PinCommandService service;

  /** Injects the PIN command contract without persistence or hashing details. */
  public PinSessionsController(PinCommandService service, IdentityErrorResponseAssembler errors) {
    this.errors = errors;
    this.service = service;
  }

  /** Translates validated HTTP input and maps expected application failures. */
  @PostMapping
  public ResponseEntity<?> signIn(@Valid @RequestBody PinSignInResource resource) {
    var session =
        service.authenticate(AuthenticateWithPinCommandFromResourceAssembler.toCommand(resource));
    return ResponseEntityAssembler.toResponseEntityFromResult(
        errors.localize(session),
        PinSessionResourceFromResultAssembler::toResource,
        HttpStatus.OK,
        IdentityErrorResponseAssembler::status);
  }
}
