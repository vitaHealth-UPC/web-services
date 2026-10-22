package com.vitahealth.tata.shared.infrastructure.i18n.configuration;

import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/** Resolves API request languages while retaining English as the deployment-independent default. */
@Configuration
public class InternationalizationConfiguration {
  /** Supports regional English and Spanish requests through language-only fallbacks. */
  @Bean
  public LocaleResolver localeResolver() {
    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
    resolver.setDefaultLocale(Locale.US);
    resolver.setSupportedLocales(
        List.of(
            Locale.US,
            Locale.ENGLISH,
            Locale.forLanguageTag("es-419"),
            Locale.forLanguageTag("es")));
    return resolver;
  }
}
