package com.vitahealth.tata.identitysubscription.interfaces.rest;

import com.vitahealth.tata.identitysubscription.application.commandservices.PasswordRecoveryCommandService;
import com.vitahealth.tata.identitysubscription.domain.model.commands.*;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.*;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.IdentityErrorResponseAssembler;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Anonymous recovery endpoints never disclose account existence or return credentials. */
@RestController
@RequestMapping("/api/v1")
public class PasswordRecoveryController {
  private final PasswordRecoveryCommandService service;
  private final IdentityErrorResponseAssembler errors;

  public PasswordRecoveryController(
      PasswordRecoveryCommandService service, IdentityErrorResponseAssembler errors) {
    this.service = service;
    this.errors = errors;
  }

  @PostMapping("/password-recovery-requests")
  public ResponseEntity<?> request(@Valid @RequestBody RequestPasswordRecoveryResource resource) {
    var result = service.request(new RequestPasswordRecoveryCommand(resource.email()));
    return response(result, HttpStatus.ACCEPTED);
  }

  @PostMapping("/password-recoveries")
  public ResponseEntity<?> complete(@Valid @RequestBody CompletePasswordRecoveryResource resource) {
    var result =
        service.complete(
            new CompletePasswordRecoveryCommand(
                resource.email(), resource.code(), resource.newPassword()));
    return response(result, HttpStatus.NO_CONTENT);
  }

  private ResponseEntity<?> response(Result<Boolean, ApplicationError> result, HttpStatus success) {
    return switch (result) {
      case Result.Success<Boolean, ApplicationError> ignored ->
          ResponseEntity.status(success).build();
      case Result.Failure<Boolean, ApplicationError> failure -> errors.from(failure.error());
    };
  }
}
