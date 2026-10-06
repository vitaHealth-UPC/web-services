package com.tata.familymonitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Follow-up note written by a caregiver")
public record CaregiverNoteResource(
    @Schema(example = "1") Long id,
    @Schema(example = "I called her and she had already taken the pill.") String text,
    @Schema(example = "2026-10-05T14:10:00Z") Instant recordedAt,
    @Schema(example = "1") Long familiarId) {
}
