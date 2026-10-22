package com.vitahealth.tata.accessibilitypreferences.interfaces.rest;

import com.vitahealth.tata.accessibilitypreferences.application.commandservices.NotificationPreferencesCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateNotificationPolicyCommandService;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.UpdateNotificationPolicyResource;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.UpdateQuietHoursResource;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.UserPreferencesResource;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.transform.PreferencesResponseAssembler;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.transform.UpdateNotificationPolicyCommandFromResourceAssembler;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.transform.UpdateNotificationPreferencesCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes owner-scoped notification choices and timing/contact configuration. */
@RestController
@RequestMapping("/api/v1/users/{userId}/notification-preferences")
@Tag(
    name = "Notification Preferences",
    description = "Quiet hours and notification channels of a user")
public class NotificationPreferencesController {
  private final PreferencesResponseAssembler responses;
  private final NotificationPreferencesCommandService commands;
  private final UpdateNotificationPolicyCommandService policies;

  /** Injects public application contracts independently of their internal handlers. */
  public NotificationPreferencesController(
      NotificationPreferencesCommandService commands,
      UpdateNotificationPolicyCommandService policies,
      PreferencesResponseAssembler responses) {
    this.responses = responses;
    this.commands = commands;
    this.policies = policies;
  }

  /** Replaces the user's quiet interval and channels through one atomic application decision. */
  @Operation(
      summary = "Set quiet hours and notification channels",
      description =
          "Replaces the saved quiet interval and channel choices together (US-39). "
              + "Send quietHours as null to remove the interval; each channel requires enabled.")
  @ApiResponse(
      responseCode = "200",
      description = "Preferences saved",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = UserPreferencesResource.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Invalid interval or repeated channel",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PutMapping
  public ResponseEntity<?> update(
      @PathVariable String userId, @Valid @RequestBody UpdateQuietHoursResource resource) {
    return responses.toResponse(
        commands.update(
            UpdateNotificationPreferencesCommandFromResourceAssembler.toCommand(userId, resource)));
  }

  /** Replaces notification timing and contacts without modifying the user's other preferences. */
  @Operation(
      summary = "Replace notification timing and emergency contacts",
      description =
          "Stores user delivery preferences without changing quiet hours, channels or treatment"
              + " dosage. Delays increase from reminder through escalation and are measured from"
              + " the scheduled intake. A null dailySummaryTime disables the summary. Saving a"
              + " contact does not grant care access.")
  @ApiResponse(
      responseCode = "200",
      description = "Policy and contacts saved",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = UserPreferencesResource.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Invalid timing or contacts",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PutMapping("/policy")
  public ResponseEntity<?> updatePolicy(
      @PathVariable String userId, @Valid @RequestBody UpdateNotificationPolicyResource resource) {
    return responses.toResponse(
        policies.handle(
            UpdateNotificationPolicyCommandFromResourceAssembler.toCommand(userId, resource)));
  }
}
