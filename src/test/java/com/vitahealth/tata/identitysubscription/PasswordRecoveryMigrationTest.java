package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** Runs the actual migration and its constraints, independently of Hibernate schema generation. */
class PasswordRecoveryMigrationTest {
  @Test
  void recoveryMigrationRejectsInvalidPersistedEvidence() throws Exception {
    try (var connection =
        DriverManager.getConnection(
            "jdbc:h2:mem:recovery_migration_" + UUID.randomUUID() + ";MODE=PostgreSQL")) {
      try (var statement = connection.createStatement()) {
        statement.execute("create table identity_accounts (id varchar(255) primary key)");
        statement.execute("insert into identity_accounts values ('account')");
      }
      ScriptUtils.executeSqlScript(
          connection, new ClassPathResource("db/migration/V19__identity_password_recoveries.sql"));
      try (var statement = connection.createStatement()) {
        statement.execute(
            "insert into identity_password_recoveries values ('account','encoded',timestamp with"
                + " time zone '2026-10-26 12:00:00+00',timestamp with time zone '2026-10-26"
                + " 12:10:00+00',0,null)");
        for (var mutation :
            new String[] {
              "failed_attempts = 6",
              "expires_at = issued_at",
              "consumed_at = expires_at",
              "failed_attempts = 5, consumed_at = issued_at"
            }) {
          var failure =
              assertThrows(
                  SQLException.class,
                  () ->
                      statement.executeUpdate(
                          "update identity_password_recoveries set "
                              + mutation
                              + " where account_id = 'account'"));
          assertEquals("23513", failure.getSQLState());
        }
        assertThrows(
            SQLException.class,
            () ->
                statement.executeUpdate(
                    "update identity_password_recoveries set account_id = 'missing'"));
      }
    }
  }
}
