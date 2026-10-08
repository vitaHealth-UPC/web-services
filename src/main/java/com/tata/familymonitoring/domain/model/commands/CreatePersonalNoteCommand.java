package com.tata.familymonitoring.domain.model.commands;

import com.tata.familymonitoring.domain.model.entities.PersonalNote.Category;

public record CreatePersonalNoteCommand(
    String olderAdultId, String title, String text, Category category) {}
