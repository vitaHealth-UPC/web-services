package com.vitahealth.tata.familymonitoring.interfaces.rest.resources;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.ContactChannelType;
import io.swagger.v3.oas.annotations.media.Schema;

/** Exposes the available contact destination for an authorized caregiver. */
@Schema(description = "Channel to contact the older adult")
public record ContactChannelResource(
    @Schema(example = "PHONE") ContactChannelType type,
    @Schema(example = "+51 999 888 777") String value) {}
