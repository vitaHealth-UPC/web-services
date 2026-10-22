package com.vitahealth.tata.identitysubscription.interfaces.rest;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.commandservices.SessionCommandService;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.CurrentSessionResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.CurrentSessionResourceFromIdentityAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the verified session identity and revocation of the caller's credential. */
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionLifecycleController {
  private final SessionCommandService commands;

  /** Injects the credential revocation contract. */
  public SessionLifecycleController(SessionCommandService commands) {
    this.commands = commands;
  }

  /** Returns the identity carried by the security principal, without exposing its token. */
  @GetMapping("/current")
  public CurrentSessionResource current(
      @AuthenticationPrincipal(expression = "session()") AuthenticatedSession session) {
    return CurrentSessionResourceFromIdentityAssembler.toResource(session);
  }

  /** Revokes the authenticated request credential; later requests must authenticate again. */
  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revoke(@RequestHeader("Authorization") String authorization) {
    commands.revoke(authorization.substring(7));
  }
}
