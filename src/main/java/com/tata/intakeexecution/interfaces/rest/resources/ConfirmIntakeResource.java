package com.tata.intakeexecution.interfaces.rest.resources;

import com.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import jakarta.validation.constraints.NotNull;

public record ConfirmIntakeResource(
        @NotNull ConfirmationChannel channel
) {}
