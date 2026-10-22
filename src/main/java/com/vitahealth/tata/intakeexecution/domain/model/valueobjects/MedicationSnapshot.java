package com.vitahealth.tata.intakeexecution.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Immutable medication evidence captured with an intake, within its persisted text bounds. */
public record MedicationSnapshot(String name, String dose, String instructions) {
  /**
   * Normalizes required text without interpreting a dose or rewriting definitive intake history.
   */
  public MedicationSnapshot {
    name = requireText(name, "medication name");
    dose = requireText(dose, "dose");
    instructions = instructions == null ? "" : instructions.trim();
    if (name.length() > 160 || dose.length() > 100 || instructions.length() > 500)
      throw new IllegalArgumentException("medication snapshot exceeds its text limits");
  }
}
