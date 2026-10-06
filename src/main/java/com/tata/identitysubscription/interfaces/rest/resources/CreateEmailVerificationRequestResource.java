package com.tata.identitysubscription.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateEmailVerificationRequestResource(
        @NotBlank @Email String email
) {}
