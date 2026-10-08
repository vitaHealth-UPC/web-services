package com.tata.familymonitoring.interfaces.rest.resources;

import com.tata.familymonitoring.domain.model.entities.PersonalNote.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePersonalNoteResource(
    @NotBlank @Size(max = 100) String title,
    @NotBlank @Size(max = 1000) String text,
    @NotNull Category category) {}
