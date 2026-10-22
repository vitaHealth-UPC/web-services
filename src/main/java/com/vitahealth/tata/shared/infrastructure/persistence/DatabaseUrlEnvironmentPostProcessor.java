package com.vitahealth.tata.shared.infrastructure.persistence;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Maps Neon/Railway {@code DATABASE_URL=postgresql://user:pass@host/db?sslmode=require} into Spring
 * JDBC properties before the datasource starts.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
  /**
   * Adds derived connection properties only when a provider URL is available and no explicit
   * datasource environment variable is configured. Invalid provider URLs leave fallback settings
   * intact; credentials are never included in diagnostics.
   */
  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    String databaseUrl = configuredValue(environment, "DATABASE_URL");
    if (databaseUrl == null) {
      return;
    }
    // Already configured explicitly for Spring.
    if (configuredValue(environment, "SPRING_DATASOURCE_URL") != null) {
      return;
    }

    try {
      String normalized = databaseUrl.trim();
      if (normalized.startsWith("jdbc:")) {
        normalized = normalized.substring("jdbc:".length());
      }
      URI uri = URI.create(normalized);
      if (!"postgresql".equalsIgnoreCase(uri.getScheme())
          && !"postgres".equalsIgnoreCase(uri.getScheme())) {
        return;
      }
      if (uri.getFragment() != null || uri.getPort() == 0 || uri.getPort() > 65535) {
        return;
      }
      String userInfo = uri.getRawUserInfo();
      if (userInfo == null || !userInfo.contains(":")) {
        return;
      }
      String[] credentials = userInfo.split(":", 2);
      String host = uri.getHost();
      int port = uri.getPort() > 0 ? uri.getPort() : 5432;
      String path = uri.getRawPath() == null ? "" : uri.getRawPath().replaceFirst("^/", "");
      if (host == null || path.isBlank()) {
        return;
      }

      // Keep sslmode; drop channel_binding (can break some JDBC clients).
      String query = sanitizeQuery(uri.getRawQuery());
      String jdbcHost = host.contains(":") && !host.startsWith("[") ? "[" + host + "]" : host;
      String jdbcUrl = "jdbc:postgresql://" + jdbcHost + ":" + port + "/" + path;
      if (query != null) {
        jdbcUrl = jdbcUrl + "?" + query;
      }

      Map<String, Object> properties = new HashMap<>();
      properties.put("spring.datasource.url", jdbcUrl);
      properties.put("spring.datasource.username", decode(credentials[0]));
      properties.put("spring.datasource.password", decode(credentials[1]));
      properties.put("SPRING_DATASOURCE_URL", jdbcUrl);
      properties.put("SPRING_DATASOURCE_USERNAME", decode(credentials[0]));
      properties.put("SPRING_DATASOURCE_PASSWORD", decode(credentials[1]));
      properties.put("DATABASE_HOST", host);
      properties.put("DATABASE_PORT", String.valueOf(port));
      properties.put("DATABASE_NAME", decode(path));
      properties.put("DATABASE_USER", decode(credentials[0]));
      properties.put("DATABASE_PASSWORD", decode(credentials[1]));
      environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", properties));
    } catch (RuntimeException ignored) {
      // Fall back to discrete SPRING_DATASOURCE_* / DATABASE_* variables.
    }
  }

  /** Preserves encoded provider options and supplies TLS when no sslmode key is present. */
  private static String sanitizeQuery(String rawQuery) {
    if (rawQuery == null || rawQuery.isBlank()) {
      return "sslmode=require";
    }
    StringBuilder query = new StringBuilder();
    boolean hasSslMode = false;
    for (String part : rawQuery.split("&")) {
      String key = decode(part.split("=", 2)[0]);
      if (part.isBlank() || "channel_binding".equals(key)) {
        continue;
      }
      hasSslMode |= "sslmode".equals(key);
      if (!query.isEmpty()) {
        query.append('&');
      }
      query.append(part);
    }
    if (query.isEmpty()) {
      return "sslmode=require";
    }
    if (!hasSslMode) {
      query.append("&sslmode=require");
    }
    return query.toString();
  }

  /** Decodes URI components without treating literal plus signs as form-encoded spaces. */
  private static String decode(String value) {
    // URI components use percent encoding; a literal plus is not a form-encoded space.
    return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
  }

  /** Reads the winning Spring property source, including system environment variables. */
  private static String configuredValue(ConfigurableEnvironment environment, String propertyName) {
    String value = environment.getProperty(propertyName);
    return value == null || value.isBlank() ? null : value.strip();
  }

  /** Runs before datasource initialization so placeholders can use the derived properties. */
  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }
}
