package com.tata.shared.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class DatabaseUrlEnvironmentPostProcessorTest {
    @Test
    void mapsRailwayUrlWithSslQueryIntoJdbcProperties() {
        var environment = new MockEnvironment();
        environment.setProperty(
                "DATABASE_URL",
                "postgresql://tata:p%40ss@db.example:5432/tata_prod?sslmode=require"
        );

        new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertEquals(
                "jdbc:postgresql://db.example:5432/tata_prod?sslmode=require",
                environment.getProperty("spring.datasource.url")
        );
        assertEquals("tata", environment.getProperty("spring.datasource.username"));
        assertEquals("p@ss", environment.getProperty("spring.datasource.password"));
        assertEquals("db.example", environment.getProperty("DATABASE_HOST"));
        assertEquals("5432", environment.getProperty("DATABASE_PORT"));
        assertEquals("tata_prod", environment.getProperty("DATABASE_NAME"));
    }

    @Test
    void ignoresMalformedDatabaseUrl() {
        var environment = new MockEnvironment();
        environment.setProperty("DATABASE_URL", "not-a-uri");

        new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertNull(environment.getProperty("spring.datasource.url"));
    }
}
