package com.vitahealth.tata.identitysubscription.interfaces.rest;

import com.vitahealth.tata.identitysubscription.application.commandservices.AccountCommandService;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.RegisterAccountResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.VerifyEmailResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.AccountResourceFromResultAssembler;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.IdentityErrorResponseAssembler;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.RegisterFamilyAccountCommandFromResourceAssembler;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.VerifiedAccountResourceFromResultAssembler;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.VerifyEmailCommandFromResourceAssembler;
import com.vitahealth.tata.shared.interfaces.rest.transform.ResponseEntityAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes account registration and email verification through explicit result contracts. */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountsController {
  private final IdentityErrorResponseAssembler errors;
  private final AccountCommandService service;

  /** Injects account commands independently of persistence and external delivery details. */
  public AccountsController(AccountCommandService service, IdentityErrorResponseAssembler errors) {
    this.errors = errors;
    this.service = service;
  }

  /** Registers a pending account and its durable verification intention. */
  @PostMapping
  public ResponseEntity<?> register(@Valid @RequestBody RegisterAccountResource resource) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        errors.localize(
            service.register(
                RegisterFamilyAccountCommandFromResourceAssembler.toCommand(resource))),
        AccountResourceFromResultAssembler::toResource,
        HttpStatus.CREATED,
        IdentityErrorResponseAssembler::status);
  }

  /** Verifies the current challenge and returns the enabled account's issued credential. */
  @PostMapping("/verification")
  public ResponseEntity<?> verify(@Valid @RequestBody VerifyEmailResource resource) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        errors.localize(
            service.verifyAndAuthenticate(
                VerifyEmailCommandFromResourceAssembler.toCommand(resource))),
        VerifiedAccountResourceFromResultAssembler::toResource,
        HttpStatus.OK,
        IdentityErrorResponseAssembler::status);
  }
}
