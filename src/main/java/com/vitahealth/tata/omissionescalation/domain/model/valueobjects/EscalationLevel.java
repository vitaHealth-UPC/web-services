package com.vitahealth.tata.omissionescalation.domain.model.valueobjects;

/** Attention level reached by an omission case. Level 0 means nothing has been escalated yet. */
public record EscalationLevel(int value) {

  public static final int MAX_VALUE = 3;
  public static final EscalationLevel NONE = new EscalationLevel(0);

  /** Accepts only the initial level and the three supported attention levels. */
  public EscalationLevel {
    if (value < 0 || value > MAX_VALUE) {
      throw new IllegalArgumentException("level must be between 0 and " + MAX_VALUE);
    }
  }

  /** Returns the next supported level; advancing beyond the maximum is rejected. */
  public EscalationLevel next() {
    return new EscalationLevel(value + 1);
  }

  /** Reports whether the final attention level has been reached. */
  public boolean isMax() {
    return value == MAX_VALUE;
  }
}
