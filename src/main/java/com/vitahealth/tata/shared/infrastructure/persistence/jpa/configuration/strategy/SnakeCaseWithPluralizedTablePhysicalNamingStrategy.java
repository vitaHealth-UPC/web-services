package com.vitahealth.tata.shared.infrastructure.persistence.jpa.configuration.strategy;

import static io.github.encryptorcode.pluralize.Pluralize.pluralize;

import java.util.Locale;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/** Maps identifiers to stable snake case and plural table names, preserving explicit quoting. */
public class SnakeCaseWithPluralizedTablePhysicalNamingStrategy implements PhysicalNamingStrategy {
  /** Preserves the configured catalog identifier and its quoting without changing the database. */
  @Override
  public Identifier toPhysicalCatalogName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
    return identifier;
  }

  /** Converts a present schema identifier to snake case while retaining explicit quoting. */
  @Override
  public Identifier toPhysicalSchemaName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
    return toSnakeCase(identifier);
  }

  /** Pluralizes a present table identifier and converts it to the configured snake case format. */
  @Override
  public Identifier toPhysicalTableName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
    return toSnakeCase(toPlural(identifier));
  }

  /** Converts a present sequence identifier to snake case without pluralizing it. */
  @Override
  public Identifier toPhysicalSequenceName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
    return toSnakeCase(identifier);
  }

  /** Converts a present column identifier to snake case without changing its quoting. */
  @Override
  public Identifier toPhysicalColumnName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
    return toSnakeCase(identifier);
  }

  private Identifier toSnakeCase(Identifier identifier) {
    if (identifier == null) return null;
    String snakeCase =
        identifier.getText().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    return Identifier.toIdentifier(snakeCase, identifier.isQuoted());
  }

  private Identifier toPlural(Identifier identifier) {
    if (identifier == null) return null;
    return Identifier.toIdentifier(pluralize(identifier.getText()), identifier.isQuoted());
  }
}
