package com.vitahealth.tata.intakeexecution.domain.model.valueobjects;

import java.util.Locale;

/**
 * Explicit supported voice locales; provider-specific aliases are translated outside the domain.
 */
public record VoiceConfirmationLanguage(String code) {
  /**
   * Canonicalizes only supported Spanish and English input without guessing an unknown language.
   */
  public VoiceConfirmationLanguage {
    if (code == null) throw new IllegalArgumentException("voice language is required");
    code =
        switch (code.trim().toLowerCase(Locale.ROOT)) {
          case "es-419" -> "es-419";
          case "es-pe" -> "es-PE";
          case "es-es" -> "es-ES";
          case "en-us" -> "en-US";
          default -> throw new IllegalArgumentException("unsupported voice confirmation language");
        };
  }
}
