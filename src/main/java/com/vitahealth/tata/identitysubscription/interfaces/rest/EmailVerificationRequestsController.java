package com.vitahealth.tata.identitysubscription.interfaces.rest;

import com.vitahealth.tata.identitysubscription.application.commandservices.AccountCommandService;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.CreateEmailVerificationRequestResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.AccountResourceFromResultAssembler;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.CreateEmailVerificationRequestCommandFromResourceAssembler;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.IdentityErrorResponseAssembler;
import com.vitahealth.tata.shared.interfaces.rest.transform.ResponseEntityAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Accepts renewed verification requests without performing provider delivery in the HTTP call. */
@RestController
@RequestMapping("/api/v1/email-verification-requests")
public class EmailVerificationRequestsController {
  private final IdentityErrorResponseAssembler errors;
  private final AccountCommandService service;

  /** Injects the account challenge command contract. */
  public EmailVerificationRequestsController(
      AccountCommandService service, IdentityErrorResponseAssembler errors) {
    this.errors = errors;
    this.service = service;
  }

  /** Renews the pending challenge and queues its encrypted delivery payload transactionally. */
  @PostMapping
  public ResponseEntity<?> create(
      @Valid @RequestBody CreateEmailVerificationRequestResource resource) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        errors.localize(
            service.requestNewVerification(
                CreateEmailVerificationRequestCommandFromResourceAssembler.toCommandFromResource(
                    resource))),
        AccountResourceFromResultAssembler::toResource,
        HttpStatus.ACCEPTED,
        IdentityErrorResponseAssembler::status);
  }
}
