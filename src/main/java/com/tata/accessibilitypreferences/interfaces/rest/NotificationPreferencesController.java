package com.tata.accessibilitypreferences.interfaces.rest;

import com.tata.accessibilitypreferences.application.internal.commandservices.UpdateNotificationChannelsCommandHandler;
import com.tata.accessibilitypreferences.application.internal.commandservices.UpdateQuietHoursCommandHandler;
import com.tata.accessibilitypreferences.interfaces.rest.resources.UpdateQuietHoursResource;
import com.tata.accessibilitypreferences.interfaces.rest.resources.UserPreferencesResource;
import com.tata.accessibilitypreferences.interfaces.rest.transform.UpdateQuietHoursCommandFromResourceAssembler;
import com.tata.accessibilitypreferences.interfaces.rest.transform.UserPreferencesResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/notification-preferences")
@Tag(name = "Notification Preferences", description = "Quiet hours and notification channels of a caregiver")
public class NotificationPreferencesController {
    private final UpdateQuietHoursCommandHandler updateQuietHours;
    private final UpdateNotificationChannelsCommandHandler updateChannels;

    public NotificationPreferencesController(
            UpdateQuietHoursCommandHandler updateQuietHours,
            UpdateNotificationChannelsCommandHandler updateChannels) {
        this.updateQuietHours = updateQuietHours;
        this.updateChannels = updateChannels;
    }

    @Operation(
            summary = "Set quiet hours and notification channels",
            description = "Replaces both settings. Non-critical notices are held back inside the quiet hours and "
                    + "only enabled channels are used (US-39). Send quietHours as null to remove them.")
    @ApiResponse(responseCode = "200", description = "Preferences saved")
    @ApiResponse(responseCode = "400", description = "Invalid interval or repeated channel",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping
    @org.springframework.transaction.annotation.Transactional
    public UserPreferencesResource update(
            @PathVariable String userId, @Valid @RequestBody UpdateQuietHoursResource resource) {
        // both commands are built first so an invalid interval is rejected before any change is saved
        var quietHoursCommand = UpdateQuietHoursCommandFromResourceAssembler.toQuietHoursCommand(userId, resource);
        var channelsCommand = UpdateQuietHoursCommandFromResourceAssembler.toChannelsCommand(userId, resource);
        updateChannels.handle(channelsCommand);
        return UserPreferencesResourceFromEntityAssembler.toResourceFromEntity(
                updateQuietHours.handle(quietHoursCommand));
    }
}
