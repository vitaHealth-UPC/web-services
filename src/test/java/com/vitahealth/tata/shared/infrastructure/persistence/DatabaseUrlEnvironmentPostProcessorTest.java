package com.vitahealth.tata.shared.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.env.MockEnvironment;

class DatabaseUrlEnvironmentPostProcessorTest {
  @Test
  void bootDiscoversTheProcessorThroughTheCurrentFactoryContract() {
    var processors =
        SpringFactoriesLoader.loadFactoryNames(
            EnvironmentPostProcessor.class, getClass().getClassLoader());
    assertEquals(
        1,
        processors.stream()
            .filter(DatabaseUrlEnvironmentPostProcessor.class.getName()::equals)
            .count());
  }

  @Test
  void providerUrlUsesTheWinningSpringPropertySource() {
    var environment =
        new MockEnvironment()
            .withProperty("DATABASE_URL", "postgresql://fallback:secret@fallback.example/tata");
    environment
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "deploymentOverride",
                Map.of("DATABASE_URL", "postgresql://selected:secret@selected.example/tata")));

    new DatabaseUrlEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());

    assertEquals("selected", environment.getProperty("spring.datasource.username"));
    assertEquals(
        "jdbc:postgresql://selected.example:5432/tata?sslmode=require",
        environment.getProperty("spring.datasource.url"));
  }

  @Test
  void preservesLiteralAndEncodedPlusInCredentials() {
    var environment =
        new MockEnvironment()
            .withProperty("DATABASE_URL", "postgres://user+name:p+%2B%3Ass@db.example/tata");
    new DatabaseUrlEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());
    assertEquals("user+name", environment.getProperty("spring.datasource.username"));
    assertEquals("p++:ss", environment.getProperty("spring.datasource.password"));
  }

  @Test
  void preservesIpv6AndEncodedDatabasePath() {
    var environment =
        new MockEnvironment()
            .withProperty("DATABASE_URL", "postgresql://user:secret@[::1]:5433/tata%20data");
    new DatabaseUrlEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());
    assertEquals(
        "jdbc:postgresql://[::1]:5433/tata%20data?sslmode=require",
        environment.getProperty("spring.datasource.url"));
    assertEquals("tata data", environment.getProperty("DATABASE_NAME"));
  }

  @Test
  void ignoresOtherSchemesAndInvalidPorts() {
    for (var url :
        new String[] {
          "https://user:secret@db.example/tata",
          "postgresql://user:secret@db.example:0/tata",
          "postgresql://user:secret@db.example:99999/tata"
        }) {
      var environment = new MockEnvironment().withProperty("DATABASE_URL", url);
      new DatabaseUrlEnvironmentPostProcessor()
          .postProcessEnvironment(environment, new SpringApplication());
      assertNull(environment.getProperty("spring.datasource.url"));
    }
  }

  @Test
  void queryValuesDoNotMasqueradeAsSslModeKeys() {
    var environment =
        new MockEnvironment()
            .withProperty(
                "DATABASE_URL",
                "postgresql://user:secret@db.example/tata?application_name=sslmode=disable");
    new DatabaseUrlEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());
    assertEquals(
        "jdbc:postgresql://db.example:5432/tata?application_name=sslmode=disable&sslmode=require",
        environment.getProperty("spring.datasource.url"));
  }

  @Test
  void explicitDatasourceUrlHasPriority() {
    var environment =
        new MockEnvironment()
            .withProperty("DATABASE_URL", "postgresql://user:secret@db.example/tata")
            .withProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://explicit/tata");
    new DatabaseUrlEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());
    assertEquals(
        "jdbc:postgresql://explicit/tata", environment.getProperty("SPRING_DATASOURCE_URL"));
    assertNull(environment.getProperty("spring.datasource.password"));
  }

  @Test
  void mapsRailwayUrlWithSslQueryIntoJdbcProperties() {
    var environment = new MockEnvironment();
    environment.setProperty(
        "DATABASE_URL", "postgresql://tata:p%40ss@db.example:5432/tata_prod?sslmode=require");

    new DatabaseUrlEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());

    assertEquals(
        "jdbc:postgresql://db.example:5432/tata_prod?sslmode=require",
        environment.getProperty("spring.datasource.url"));
    assertEquals(
        "jdbc:postgresql://db.example:5432/tata_prod?sslmode=require",
        environment.getProperty("SPRING_DATASOURCE_URL"));
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

    new DatabaseUrlEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());

    assertNull(environment.getProperty("spring.datasource.url"));
  }

  @Test
  void dropsChannelBindingFromNeonUrl() {
    var environment = new MockEnvironment();
    environment.setProperty(
        "DATABASE_URL",
        "postgresql://tata_owner:secret@ep-demo-pooler.neon.tech/tata?sslmode=require&channel_binding=require");

    new DatabaseUrlEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());

    assertEquals(
        "jdbc:postgresql://ep-demo-pooler.neon.tech:5432/tata?sslmode=require",
        environment.getProperty("spring.datasource.url"));
  }
}
