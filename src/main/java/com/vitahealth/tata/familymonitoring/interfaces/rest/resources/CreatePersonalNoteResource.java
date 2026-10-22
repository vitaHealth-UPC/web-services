package com.vitahealth.tata.familymonitoring.interfaces.rest.resources;

import com.vitahealth.tata.familymonitoring.domain.model.entities.PersonalNote;
import com.vitahealth.tata.familymonitoring.domain.model.entities.PersonalNote.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Accepts private reminder content; ownership is always obtained from the session. */
public record CreatePersonalNoteResource(
    @NotBlank @Size(max = PersonalNote.MAX_TITLE_LENGTH) String title,
    @NotBlank @Size(max = PersonalNote.MAX_TEXT_LENGTH) String text,
    @NotNull Category category) {}
