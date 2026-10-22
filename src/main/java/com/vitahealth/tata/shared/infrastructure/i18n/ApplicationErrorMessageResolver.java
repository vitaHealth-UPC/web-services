package com.vitahealth.tata.shared.infrastructure.i18n;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import java.util.Locale;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/** Resolves context-owned public error messages without changing stable machine-readable codes. */
@Component
public final class ApplicationErrorMessageResolver {
  private final MessageSource messages;

  /** Injects configured resource bundles rather than coupling domain errors to request language. */
  public ApplicationErrorMessageResolver(MessageSource messages) {
    this.messages = messages;
  }

  /**
   * Localizes one public application failure, preserving its message when no catalog key exists.
   *
   * @param namespace context-owned message prefix, such as identity.error
   * @param error public failure whose code and diagnostic details remain unchanged
   * @param locale requested API language
   * @return failure with the original code and localized public message
   */
  public ApplicationError resolve(String namespace, ApplicationError error, Locale locale) {
    Objects.requireNonNull(namespace, "namespace");
    Objects.requireNonNull(error, "error");
    Objects.requireNonNull(locale, "locale");
    var key = namespace + "." + error.code().toLowerCase(Locale.ROOT).replace('_', '-');
    return new ApplicationError(
        error.code(), messages.getMessage(key, null, error.message(), locale), error.details());
  }
}
