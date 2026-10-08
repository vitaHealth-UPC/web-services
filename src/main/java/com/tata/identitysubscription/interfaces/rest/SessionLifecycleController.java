package com.tata.identitysubscription.interfaces.rest;
import com.tata.identitysubscription.application.commandservices.SessionCommandService;
import com.tata.identitysubscription.application.models.AuthenticatedSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionLifecycleController {
 private final SessionCommandService commands;
 public SessionLifecycleController(SessionCommandService commands) { this.commands=commands; }
 @GetMapping("/current") public AuthenticatedSession current(@AuthenticationPrincipal AuthenticatedSession session) { return session; }
 @DeleteMapping @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
 public void revoke(@RequestHeader("Authorization") String authorization) { commands.revoke(authorization.substring(7)); }
}
