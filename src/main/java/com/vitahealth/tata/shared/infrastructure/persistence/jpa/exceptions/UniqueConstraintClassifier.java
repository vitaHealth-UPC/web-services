package com.vitahealth.tata.shared.infrastructure.persistence.jpa.exceptions;

import java.util.Locale;
import java.util.regex.Pattern;
import org.hibernate.exception.ConstraintViolationException;

/**
 * Identifies specific unique constraints from structured Hibernate metadata without parsing SQL.
 */
public final class UniqueConstraintClassifier {
  private UniqueConstraintClassifier() {}

  /**
   * Requires both SQLState 23505 and the named constraint, including H2's index metadata format.
   */
  public static boolean matches(Throwable failure, String constraintName) {
    if (constraintName == null || constraintName.isBlank()) {
      throw new IllegalArgumentException("constraintName is required");
    }
    String namePattern = Pattern.quote(constraintName.toLowerCase(Locale.ROOT));
    String pattern =
        "(?:public\\.)?"
            + namePattern
            + "(?:_index_[0-9a-f]+| index (?:public\\.)?"
            + namePattern
            + "_index_[0-9a-f]+)?";
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation) {
        String name = violation.getConstraintName();
        return "23505".equals(violation.getSQLState())
            && name != null
            && name.toLowerCase(Locale.ROOT).matches(pattern);
      }
    }
    return false;
  }
}
