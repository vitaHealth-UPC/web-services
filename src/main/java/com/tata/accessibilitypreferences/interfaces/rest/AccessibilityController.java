package com.tata.accessibilitypreferences.interfaces.rest;

import com.tata.accessibilitypreferences.application.internal.commandservices.UpdateContrastCommandHandler;
import com.tata.accessibilitypreferences.application.internal.commandservices.UpdateReadingAssistanceCommandHandler;
import com.tata.accessibilitypreferences.application.internal.commandservices.UpdateReducedMotionCommandHandler;
import com.tata.accessibilitypreferences.application.internal.commandservices.UpdateTextSizeCommandHandler;
import com.tata.accessibilitypreferences.application.internal.commandservices.UpdateVoiceConfirmationCommandHandler;
import com.tata.accessibilitypreferences.application.internal.queryservices.GetUserPreferencesQueryHandler;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateContrastCommand;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateReadingAssistanceCommand;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateReducedMotionCommand;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateVoiceConfirmationCommand;
import com.tata.accessibilitypreferences.domain.model.queries.GetUserPreferencesQuery;
import com.tata.accessibilitypreferences.interfaces.rest.resources.UpdateContrastResource;
import com.tata.accessibilitypreferences.interfaces.rest.resources.UpdateReadingAssistanceResource;
import com.tata.accessibilitypreferences.interfaces.rest.resources.UpdateReducedMotionResource;
import com.tata.accessibilitypreferences.interfaces.rest.resources.UpdateTextSizeResource;
import com.tata.accessibilitypreferences.interfaces.rest.resources.UpdateVoiceConfirmationResource;
import com.tata.accessibilitypreferences.interfaces.rest.resources.UserPreferencesResource;
import com.tata.accessibilitypreferences.interfaces.rest.transform.UpdateTextSizeCommandFromResourceAssembler;
import com.tata.accessibilitypreferences.interfaces.rest.transform.UserPreferencesResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/preferences")
@Tag(name = "Accessibility", description = "Text size, contrast, motion, reading help and voice confirmation")
public class AccessibilityController {
    private final GetUserPreferencesQueryHandler getPreferences;
    private final UpdateTextSizeCommandHandler updateTextSize;
    private final UpdateContrastCommandHandler updateContrast;
    private final UpdateReducedMotionCommandHandler updateReducedMotion;
    private final UpdateReadingAssistanceCommandHandler updateReadingAssistance;
    private final UpdateVoiceConfirmationCommandHandler updateVoiceConfirmation;

    public AccessibilityController(
            GetUserPreferencesQueryHandler getPreferences,
            UpdateTextSizeCommandHandler updateTextSize,
            UpdateContrastCommandHandler updateContrast,
            UpdateReducedMotionCommandHandler updateReducedMotion,
            UpdateReadingAssistanceCommandHandler updateReadingAssistance,
            UpdateVoiceConfirmationCommandHandler updateVoiceConfirmation) {
        this.getPreferences = getPreferences;
        this.updateTextSize = updateTextSize;
        this.updateContrast = updateContrast;
        this.updateReducedMotion = updateReducedMotion;
        this.updateReadingAssistance = updateReadingAssistance;
        this.updateVoiceConfirmation = updateVoiceConfirmation;
    }

    @Operation(
            summary = "Get the preferences of a user",
            description = "Accessibility and notification preferences. A user who never saved anything "
                    + "gets the defaults, which are stored on this first read (US-35, US-36).")
    @ApiResponse(responseCode = "200", description = "Preferences returned")
    @GetMapping
    public UserPreferencesResource get(@PathVariable String userId) {
        return UserPreferencesResourceFromEntityAssembler.toResourceFromEntity(
                getPreferences.handle(new GetUserPreferencesQuery(userId)));
    }

    @Operation(summary = "Change the text size", description = "The value is kept for the next sessions (US-35).")
    @ApiResponse(responseCode = "200", description = "Text size saved")
    @ApiResponse(responseCode = "400", description = "Text size not allowed",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/text-size")
    public UserPreferencesResource updateTextSize(
            @PathVariable String userId, @Valid @RequestBody UpdateTextSizeResource resource) {
        return UserPreferencesResourceFromEntityAssembler.toResourceFromEntity(
                updateTextSize.handle(
                        UpdateTextSizeCommandFromResourceAssembler.toCommandFromResource(userId, resource)));
    }

    @Operation(summary = "Turn high contrast on or off", description = "The value is kept for the next sessions (US-36).")
    @ApiResponse(responseCode = "200", description = "Contrast preference saved")
    @PutMapping("/contrast")
    public UserPreferencesResource updateContrast(
            @PathVariable String userId, @Valid @RequestBody UpdateContrastResource resource) {
        return UserPreferencesResourceFromEntityAssembler.toResourceFromEntity(
                updateContrast.handle(new UpdateContrastCommand(userId, resource.enabled())));
    }

    @Operation(summary = "Turn reduced motion on or off", description = "The value is kept for the next sessions (US-37).")
    @ApiResponse(responseCode = "200", description = "Motion preference saved")
    @PutMapping("/reduced-motion")
    public UserPreferencesResource updateReducedMotion(
            @PathVariable String userId, @Valid @RequestBody UpdateReducedMotionResource resource) {
        return UserPreferencesResourceFromEntityAssembler.toResourceFromEntity(
                updateReducedMotion.handle(new UpdateReducedMotionCommand(userId, resource.enabled())));
    }

    @Operation(summary = "Turn reading assistance on or off", description = "The value is kept for the next sessions (US-38).")
    @ApiResponse(responseCode = "200", description = "Reading assistance preference saved")
    @PutMapping("/reading-assistance")
    public UserPreferencesResource updateReadingAssistance(
            @PathVariable String userId, @Valid @RequestBody UpdateReadingAssistanceResource resource) {
        return UserPreferencesResourceFromEntityAssembler.toResourceFromEntity(
                updateReadingAssistance.handle(new UpdateReadingAssistanceCommand(userId, resource.enabled())));
    }

    @Operation(summary = "Turn voice confirmation on or off", description = "Lets the user confirm an intake by voice (US-06).")
    @ApiResponse(responseCode = "200", description = "Voice confirmation preference saved")
    @PutMapping("/voice-confirmation")
    public UserPreferencesResource updateVoiceConfirmation(
            @PathVariable String userId, @Valid @RequestBody UpdateVoiceConfirmationResource resource) {
        return UserPreferencesResourceFromEntityAssembler.toResourceFromEntity(
                updateVoiceConfirmation.handle(new UpdateVoiceConfirmationCommand(userId, resource.enabled())));
    }
}
