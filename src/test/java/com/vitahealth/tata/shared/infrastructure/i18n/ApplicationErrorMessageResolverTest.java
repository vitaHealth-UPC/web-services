package com.vitahealth.tata.shared.infrastructure.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

/** Checks shared message lookup leaves codes, diagnostics and unmapped public errors intact. */
class ApplicationErrorMessageResolverTest {
  @Test
  void localizationPreservesMachineCodeAndDiagnosticsWithAnExplicitFallback() {
    var messages = new StaticMessageSource();
    messages.addMessage(
        "example.error.invalid-input", Locale.forLanguageTag("es"), "Entrada inválida");
    var resolver = new ApplicationErrorMessageResolver(messages);
    var error = new ApplicationError("INVALID_INPUT", "Invalid input", "field-name");
    var localized = resolver.resolve("example.error", error, Locale.forLanguageTag("es"));
    assertThat(localized.code()).isEqualTo(error.code());
    assertThat(localized.message()).isEqualTo("Entrada inválida");
    assertThat(localized.details()).isEqualTo(error.details());
    assertThat(resolver.resolve("unknown.error", error, Locale.forLanguageTag("es")))
        .isEqualTo(error);
  }
}
