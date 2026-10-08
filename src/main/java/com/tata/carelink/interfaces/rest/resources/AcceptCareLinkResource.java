package com.tata.carelink.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record AcceptCareLinkResource(
        @NotBlank String caregiverId,
        @NotBlank String code
) {}
