package com.tata.familymonitoring.interfaces.rest.resources;

import com.tata.familymonitoring.domain.model.valueobjects.ContactChannelType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Channel to contact the older adult")
public record ContactChannelResource(
    @Schema(example = "PHONE") ContactChannelType type,
    @Schema(example = "+51 999 888 777") String value) {
}
