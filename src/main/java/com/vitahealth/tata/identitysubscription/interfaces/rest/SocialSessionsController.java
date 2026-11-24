package com.vitahealth.tata.identitysubscription.interfaces.rest;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.commandservices.SocialSessionCommandService;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.SocialIdentityResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.*;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.shared.interfaces.rest.transform.ResponseEntityAssembler;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Issues local Tata sessions only after signed Firebase evidence has been verified. */
@RestController
@RequestMapping("/api/v1")
public class SocialSessionsController {
  private final SocialSessionCommandService commands;
  private final IdentityErrorResponseAssembler errors;

  public SocialSessionsController(SocialSessionCommandService commands, IdentityErrorResponseAssembler errors) {
    this.commands = commands;
    this.errors = errors;
  }

  @PostMapping("/social-sessions")
  public ResponseEntity<?> signIn(@Valid @RequestBody SocialIdentityResource request) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        errors.localize(commands.authenticate(request.firebaseIdToken())),
        AuthenticatedAccountResourceFromResultAssembler::toResource, HttpStatus.OK,
        IdentityErrorResponseAssembler::status);
  }

  /** Requires a real caregiver session. An adult or setup token cannot bind a social identity. */
  @PutMapping("/me/social-identities")
  public ResponseEntity<?> link(@Valid @RequestBody SocialIdentityResource request,
      @AuthenticationPrincipal(expression = "session()") AuthenticatedSession session) {
    if (session == null || session.role() != AuthenticatedSession.Role.CAREGIVER) {
      return errors.from(new com.vitahealth.tata.shared.application.result.ApplicationError(
          "ACCOUNT_NOT_ACTIVE", "caregiver access required"));
    }
    var result = commands.link(request.firebaseIdToken(), session.subjectId());
    return switch (errors.localize(result)) {
      case Result.Success<Boolean, com.vitahealth.tata.shared.application.result.ApplicationError> ignored ->
          ResponseEntity.noContent().build();
      case Result.Failure<Boolean, com.vitahealth.tata.shared.application.result.ApplicationError> failure ->
          errors.from(failure.error());
    };
  }
}
