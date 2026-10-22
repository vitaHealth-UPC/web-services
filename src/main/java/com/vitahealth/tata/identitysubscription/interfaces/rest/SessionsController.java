package com.vitahealth.tata.identitysubscription.interfaces.rest;

import com.vitahealth.tata.identitysubscription.application.commandservices.AccountCommandService;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.SignInResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.AuthenticateFamilyCommandFromResourceAssembler;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.AuthenticatedAccountResourceFromResultAssembler;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.IdentityErrorResponseAssembler;
import com.vitahealth.tata.shared.interfaces.rest.transform.ResponseEntityAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Translates caregiver authentication outcomes into the existing credential/error HTTP contracts.
 */
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionsController {
  private final IdentityErrorResponseAssembler errors;
  private final AccountCommandService service;

  /** Injects authentication commands without coupling HTTP handling to token infrastructure. */
  public SessionsController(AccountCommandService service, IdentityErrorResponseAssembler errors) {
    this.errors = errors;
    this.service = service;
  }

  /**
   * Authenticates caregiver input and maps expected failures without exposing credential hashes.
   */
  @PostMapping
  public ResponseEntity<?> signIn(@Valid @RequestBody SignInResource resource) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        errors.localize(
            service.authenticate(
                AuthenticateFamilyCommandFromResourceAssembler.toCommand(resource))),
        AuthenticatedAccountResourceFromResultAssembler::toResource,
        HttpStatus.OK,
        IdentityErrorResponseAssembler::status);
  }
}
