package com.tata.shared.infrastructure.persistence;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Maps Neon/Railway {@code DATABASE_URL=postgresql://user:pass@host/db?sslmode=require}
 * into Spring JDBC properties before the datasource starts.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = firstNonBlank(
                System.getenv("DATABASE_URL"),
                environment.getProperty("DATABASE_URL")
        );
        if (databaseUrl == null) {
            return;
        }
        // Already configured explicitly for Spring.
        if (firstNonBlank(System.getenv("SPRING_DATASOURCE_URL"), environment.getProperty("SPRING_DATASOURCE_URL")) != null) {
            return;
        }

        try {
            String normalized = databaseUrl.trim();
            if (normalized.startsWith("jdbc:")) {
                normalized = normalized.substring("jdbc:".length());
            }
            URI uri = URI.create(normalized);
            String userInfo = uri.getRawUserInfo();
            if (userInfo == null || !userInfo.contains(":")) {
                return;
            }
            String[] credentials = userInfo.split(":", 2);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
            if (host == null || path.isBlank()) {
                return;
            }

            // Keep sslmode; drop channel_binding (can break some JDBC clients).
            String query = sanitizeQuery(uri.getRawQuery());
            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + path;
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
            properties.put("DATABASE_NAME", path);
            properties.put("DATABASE_USER", decode(credentials[0]));
            properties.put("DATABASE_PASSWORD", decode(credentials[1]));
            environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", properties));
        } catch (RuntimeException ignored) {
            // Fall back to discrete SPRING_DATASOURCE_* / DATABASE_* variables.
        }
    }

    private static String sanitizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "sslmode=require";
        }
        StringBuilder query = new StringBuilder();
        for (String part : rawQuery.split("&")) {
            if (part.isBlank() || part.startsWith("channel_binding=")) {
                continue;
            }
            if (!query.isEmpty()) {
                query.append('&');
            }
            query.append(part);
        }
        if (query.isEmpty()) {
            return "sslmode=require";
        }
        if (!query.toString().contains("sslmode=")) {
            query.append("&sslmode=require");
        }
        return query.toString();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
