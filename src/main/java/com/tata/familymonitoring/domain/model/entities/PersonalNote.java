package com.tata.familymonitoring.domain.model.entities;

import com.tata.shared.domain.validation.DomainText;
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
  public enum Category {
    MEDICATION,
    ROUTINE
  }

  public PersonalNote {
    olderAdultId = DomainText.requireText(olderAdultId, "olderAdultId");
    title = DomainText.requireText(title, "title");
    text = DomainText.requireText(text, "text");
    if (title.length() > 100 || text.length() > 1000)
      throw new IllegalArgumentException("Personal note exceeds its maximum length");
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(recordedAt, "recordedAt");
  }
}
