package com.tata.shared.infrastructure.persistence.jpa.configuration.strategy;

import static io.github.encryptorcode.pluralize.Pluralize.pluralize;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class SnakeCaseWithPluralizedTablePhysicalNamingStrategy implements PhysicalNamingStrategy {
    @Override
    public Identifier toPhysicalCatalogName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return null;
    }
    @Override
    public Identifier toPhysicalSchemaName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return toSnakeCase(identifier);
    }
    @Override
    public Identifier toPhysicalTableName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return toSnakeCase(toPlural(identifier));
    }
    @Override
    public Identifier toPhysicalSequenceName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return toSnakeCase(identifier);
    }
    @Override
    public Identifier toPhysicalColumnName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return toSnakeCase(identifier);
    }
    private Identifier toSnakeCase(Identifier identifier) {
        if (identifier == null) return null;
        String snakeCase = identifier.getText().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
        return Identifier.toIdentifier(snakeCase, identifier.isQuoted());
    }
    private Identifier toPlural(Identifier identifier) {
        if (identifier == null) return null;
        return Identifier.toIdentifier(pluralize(identifier.getText()), identifier.isQuoted());
    }
}
