package com.tata.accessibilitypreferences.interfaces.rest.resources;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateNotificationPolicyResource(
    @NotNull @Valid NotificationPolicyResource policy,
    @NotNull @Size(max = 5) List<@NotNull @Valid EmergencyContactResource> contacts) {}
