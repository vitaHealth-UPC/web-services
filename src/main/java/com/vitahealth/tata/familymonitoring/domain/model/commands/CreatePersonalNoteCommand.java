package com.vitahealth.tata.familymonitoring.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.familymonitoring.domain.model.entities.PersonalNote;
import com.vitahealth.tata.familymonitoring.domain.model.entities.PersonalNote.Category;
import java.util.Objects;

/** Carries a new private reminder for the adult selected by the authenticated session. */
public record CreatePersonalNoteCommand(
    String olderAdultId, String title, String text, Category category) {
  /** Normalizes required input and rejects invalid lengths before invoking persistence. */
  public CreatePersonalNoteCommand {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    title = requireText(title, "title");
    text = requireText(text, "text");
    if (title.length() > PersonalNote.MAX_TITLE_LENGTH
        || text.length() > PersonalNote.MAX_TEXT_LENGTH)
      throw new IllegalArgumentException("personal note exceeds its maximum length");
    Objects.requireNonNull(category, "category");
  }
}
