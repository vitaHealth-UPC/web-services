package com.vitahealth.tata.familymonitoring.interfaces.rest.resources;

import com.vitahealth.tata.familymonitoring.domain.model.entities.PersonalNote.Category;
import java.time.Instant;

/** Represents private reminder evidence without exposing or accepting an owner selector. */
public record PersonalNoteResource(
    Long id, String title, String text, Category category, Instant recordedAt) {}
