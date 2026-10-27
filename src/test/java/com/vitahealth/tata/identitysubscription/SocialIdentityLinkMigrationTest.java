package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** Enforces unique external identity binding at the database, not just in application checks. */
class SocialIdentityLinkMigrationTest {
  @Test void socialLinkCannotBeReassignedToAnotherAccount() throws Exception {
    try (var connection = DriverManager.getConnection(
        "jdbc:h2:mem:social_link_"+UUID.randomUUID()+";MODE=PostgreSQL")) {
      try (var sql = connection.createStatement()) {
        sql.execute("create table identity_accounts (id varchar(255) primary key)");
        sql.execute("insert into identity_accounts values ('caregiver-a'), ('caregiver-b')");
      }
      ScriptUtils.executeSqlScript(
          connection, new ClassPathResource("db/migration/V21__identity_social_links.sql"));
      try (var sql = connection.createStatement()) {
        sql.execute("insert into identity_social_links values "
            + "('a','https://issuer','uid','caregiver-a','GOOGLE')");
        assertThrows(SQLException.class, () -> sql.execute(
            "insert into identity_social_links values "
                + "('b','https://issuer','uid','caregiver-b','FACEBOOK')"));
        assertThrows(SQLException.class, () -> sql.execute(
            "insert into identity_social_links values "
                + "('c','https://issuer','other','caregiver-z','GOOGLE')"));
        assertThrows(SQLException.class, () -> sql.execute(
            "insert into identity_social_links values "
                + "('d','https://issuer','other','caregiver-b','PASSWORD')"));
      }
    }
  }
}
