package com.tata.identitysubscription.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record ChangeSubscriptionResource(
        @NotBlank String planCode
) {
}
