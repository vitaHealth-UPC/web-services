package com.tata.familymonitoring.interfaces.rest.resources;

import com.tata.familymonitoring.domain.model.entities.PersonalNote.Category;
import java.time.Instant;

public record PersonalNoteResource(
    Long id, String title, String text, Category category, Instant recordedAt) {}
