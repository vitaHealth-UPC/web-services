package com.vitahealth.tata.familymonitoring.domain.model.entities;

import com.vitahealth.tata.shared.domain.validation.DomainText;
import java.time.Instant;
import java.util.Objects;

/** A private reminder owned by the older adult, separate from caregiver follow-up notes. */
public record PersonalNote(
    Long id,
    String olderAdultId,
    String title,
    String text,
    Category category,
    Instant recordedAt) {
  /** Maximum number of characters in a private reminder title. */
  public static final int MAX_TITLE_LENGTH = 100;

  /** Maximum number of characters in a private reminder body. */
  public static final int MAX_TEXT_LENGTH = 1000;

  /** Distinguishes medication reminders from routine reminders. */
  public enum Category {
    MEDICATION,
    ROUTINE
  }

  /** Normalizes new notes and validates stored notes without rewriting their original text. */
  public PersonalNote {
    if (id != null && id <= 0)
      throw new IllegalArgumentException("stored note identity must be positive");
    olderAdultId = DomainText.requireText(olderAdultId, "olderAdultId");
    var normalizedTitle = DomainText.requireText(title, "title");
    var normalizedText = DomainText.requireText(text, "text");
    if (id == null) {
      title = normalizedTitle;
      text = normalizedText;
    }
    if (title.length() > MAX_TITLE_LENGTH || text.length() > MAX_TEXT_LENGTH)
      throw new IllegalArgumentException("Personal note exceeds its maximum length");
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(recordedAt, "recordedAt");
  }
}
