package com.vitahealth.tata.accessibilitypreferences.interfaces.rest;

import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateDarkThemeCommandService;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateDarkThemeCommand;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.UpdateDarkThemeResource;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.transform.PreferencesResponseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the explicit theme choice through the same owner-scoped preference boundary. */
@RestController
@RequestMapping("/api/v1/users/{userId}/preferences")
@Tag(name = "Accessibility")
public class DarkThemeController {
  private final UpdateDarkThemeCommandService commands;
  private final PreferencesResponseAssembler responses;

  public DarkThemeController(
      UpdateDarkThemeCommandService commands, PreferencesResponseAssembler responses) {
    this.commands = commands;
    this.responses = responses;
  }

  @Operation(summary = "Change the explicit dark theme preference")
  @PutMapping("/dark-theme")
  public ResponseEntity<?> update(
      @PathVariable String userId, @Valid @RequestBody UpdateDarkThemeResource resource) {
    return responses.toResponse(
        commands.handle(new UpdateDarkThemeCommand(userId, resource.enabled())));
  }
}
