package com.tata.familymonitoring.interfaces.rest.resources;

import com.tata.familymonitoring.domain.model.entities.CaregiverNote;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register a follow-up note")
public record CreateCaregiverNoteResource(
    @Schema(description = "Caregiver who writes the note", example = "1")
    @NotNull Long familiarId,
    @Schema(example = "I called her and she had already taken the pill.")
    @NotBlank @Size(max = CaregiverNote.MAX_LENGTH) String text) {
}
