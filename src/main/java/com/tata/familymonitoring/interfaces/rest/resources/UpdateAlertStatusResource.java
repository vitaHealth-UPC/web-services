package com.tata.familymonitoring.interfaces.rest.resources;

import com.tata.familymonitoring.domain.model.valueobjects.AlertStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to move an alert forward. Only ATTENDED and CLOSED are accepted")
public record UpdateAlertStatusResource(
    @Schema(example = "ATTENDED") @NotNull AlertStatus status) {
}
