package com.vitahealth.tata.accessibilitypreferences.interfaces.rest;

import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateContrastCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateReadingAssistanceCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateReducedMotionCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateTextSizeCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateVoiceConfirmationCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.queryservices.GetUserPreferencesQueryService;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateContrastCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateReadingAssistanceCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateReducedMotionCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateVoiceConfirmationCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.queries.GetUserPreferencesQuery;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.UpdateContrastResource;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.UpdateReadingAssistanceResource;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.UpdateReducedMotionResource;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.UpdateTextSizeResource;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.UpdateVoiceConfirmationResource;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.UserPreferencesResource;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.transform.PreferencesResponseAssembler;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.transform.UpdateTextSizeCommandFromResourceAssembler;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.transform.UserPreferencesResourceFromResultAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes user-owned accessibility reads and explicit replacement choices. */
@RestController
@RequestMapping("/api/v1/users/{userId}/preferences")
@Tag(
    name = "Accessibility",
    description = "Text size, contrast, motion, reading help and voice confirmation")
public class AccessibilityController {
  private final PreferencesResponseAssembler responses;
  private final GetUserPreferencesQueryService getPreferences;
  private final UpdateTextSizeCommandService updateTextSize;
  private final UpdateContrastCommandService updateContrast;
  private final UpdateReducedMotionCommandService updateReducedMotion;
  private final UpdateReadingAssistanceCommandService updateReadingAssistance;
  private final UpdateVoiceConfirmationCommandService updateVoiceConfirmation;

  /** Injects public preference read and individual change contracts. */
  public AccessibilityController(
      GetUserPreferencesQueryService getPreferences,
      UpdateTextSizeCommandService updateTextSize,
      UpdateContrastCommandService updateContrast,
      UpdateReducedMotionCommandService updateReducedMotion,
      UpdateReadingAssistanceCommandService updateReadingAssistance,
      UpdateVoiceConfirmationCommandService updateVoiceConfirmation,
      PreferencesResponseAssembler responses) {
    this.responses = responses;
    this.getPreferences = getPreferences;
    this.updateTextSize = updateTextSize;
    this.updateContrast = updateContrast;
    this.updateReducedMotion = updateReducedMotion;
    this.updateReadingAssistance = updateReadingAssistance;
    this.updateVoiceConfirmation = updateVoiceConfirmation;
  }

  /** Reads saved preferences or non-persisted product defaults. */
  @Operation(
      summary = "Get the preferences of a user",
      description =
          "Accessibility and notification preferences. A user who never saved anything "
              + "gets product defaults without creating saved preferences (US-35, US-36).")
  @ApiResponse(
      responseCode = "200",
      description = "Preferences returned",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = UserPreferencesResource.class)))
  @GetMapping
  public UserPreferencesResource get(@PathVariable String userId) {
    return UserPreferencesResourceFromResultAssembler.toResource(
        getPreferences.handle(new GetUserPreferencesQuery(userId)));
  }

  /** Replaces text size while preserving every other preference. */
  @Operation(
      summary = "Change the text size",
      description = "The value is kept for the next sessions (US-35).")
  @ApiResponse(
      responseCode = "200",
      description = "Text size saved",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = UserPreferencesResource.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Text size not allowed",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PutMapping("/text-size")
  public ResponseEntity<?> updateTextSize(
      @PathVariable String userId, @Valid @RequestBody UpdateTextSizeResource resource) {
    return responses.toResponse(
        updateTextSize.handle(
            UpdateTextSizeCommandFromResourceAssembler.toCommandFromResource(userId, resource)));
  }

  /** Applies the user's explicit high-contrast choice. */
  @Operation(
      summary = "Turn high contrast on or off",
      description = "The value is kept for the next sessions (US-36).")
  @ApiResponse(
      responseCode = "200",
      description = "Contrast preference saved",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = UserPreferencesResource.class)))
  @PutMapping("/contrast")
  public ResponseEntity<?> updateContrast(
      @PathVariable String userId, @Valid @RequestBody UpdateContrastResource resource) {
    return responses.toResponse(
        updateContrast.handle(new UpdateContrastCommand(userId, resource.enabled())));
  }

  /** Applies the user's explicit reduced-motion choice. */
  @Operation(
      summary = "Turn reduced motion on or off",
      description = "The value is kept for the next sessions (US-37).")
  @ApiResponse(
      responseCode = "200",
      description = "Motion preference saved",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = UserPreferencesResource.class)))
  @PutMapping("/reduced-motion")
  public ResponseEntity<?> updateReducedMotion(
      @PathVariable String userId, @Valid @RequestBody UpdateReducedMotionResource resource) {
    return responses.toResponse(
        updateReducedMotion.handle(new UpdateReducedMotionCommand(userId, resource.enabled())));
  }

  /** Applies the user's explicit reading-assistance choice. */
  @Operation(
      summary = "Turn reading assistance on or off",
      description = "The value is kept for the next sessions (US-38).")
  @ApiResponse(
      responseCode = "200",
      description = "Reading assistance preference saved",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = UserPreferencesResource.class)))
  @PutMapping("/reading-assistance")
  public ResponseEntity<?> updateReadingAssistance(
      @PathVariable String userId, @Valid @RequestBody UpdateReadingAssistanceResource resource) {
    return responses.toResponse(
        updateReadingAssistance.handle(
            new UpdateReadingAssistanceCommand(userId, resource.enabled())));
  }

  /** Applies the voice choice consulted by intake confirmation. */
  @Operation(
      summary = "Turn voice confirmation on or off",
      description = "Lets the user confirm an intake by voice (US-06).")
  @ApiResponse(
      responseCode = "200",
      description = "Voice confirmation preference saved",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = UserPreferencesResource.class)))
  @PutMapping("/voice-confirmation")
  public ResponseEntity<?> updateVoiceConfirmation(
      @PathVariable String userId, @Valid @RequestBody UpdateVoiceConfirmationResource resource) {
    return responses.toResponse(
        updateVoiceConfirmation.handle(
            new UpdateVoiceConfirmationCommand(userId, resource.enabled())));
  }
}
