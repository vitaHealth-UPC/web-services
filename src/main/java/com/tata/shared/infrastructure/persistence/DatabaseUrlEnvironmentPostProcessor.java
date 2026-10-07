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
 * Maps Railway-style {@code DATABASE_URL=postgresql://user:pass@host:port/db?sslmode=require}
 * into Spring JDBC properties.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = firstNonBlank(
                environment.getProperty("DATABASE_URL"),
                System.getenv("DATABASE_URL")
        );
        if (databaseUrl == null) {
            return;
        }
        try {
            URI uri = URI.create(databaseUrl);
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

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + path;
            if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
                jdbcUrl = jdbcUrl + "?" + uri.getRawQuery();
            }

            Map<String, Object> properties = new HashMap<>();
            properties.put("spring.datasource.url", jdbcUrl);
            properties.put("spring.datasource.username", decode(credentials[0]));
            properties.put("spring.datasource.password", decode(credentials[1]));
            properties.put("DATABASE_HOST", host);
            properties.put("DATABASE_PORT", String.valueOf(port));
            properties.put("DATABASE_NAME", path);
            properties.put("DATABASE_USER", decode(credentials[0]));
            properties.put("DATABASE_PASSWORD", decode(credentials[1]));
            environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", properties));
        } catch (IllegalArgumentException ignored) {
            // Leave Spring defaults / explicit DATABASE_* variables untouched.
        }
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
