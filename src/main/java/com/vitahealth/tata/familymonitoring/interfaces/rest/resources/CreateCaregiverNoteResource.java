package com.vitahealth.tata.familymonitoring.interfaces.rest.resources;

import com.vitahealth.tata.familymonitoring.domain.model.entities.CaregiverNote;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Accepts intervention content and an author selector checked against the authenticated session.
 */
@Schema(description = "Request to register a follow-up note")
public record CreateCaregiverNoteResource(
    @Schema(
            description = "Caregiver who writes the note",
            example = "00000000-0000-0000-0000-000000000001")
        @NotBlank
        String familiarId,
    @Schema(example = "I called her and she had already taken the pill.")
        @NotBlank
        @Size(max = CaregiverNote.MAX_LENGTH)
        String text) {}
