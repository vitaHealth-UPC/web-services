package com.vitahealth.tata.familymonitoring.interfaces.rest.resources;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Accepts a requested alert transition; the controller rejects reopening an alert. */
@Schema(description = "Request to move an alert forward. Only ATTENDED and CLOSED are accepted")
public record UpdateAlertStatusResource(
    @Schema(example = "ATTENDED") @NotNull AlertStatus status) {}
