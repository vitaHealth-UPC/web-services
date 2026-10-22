package com.vitahealth.tata.shared.application.result;

import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents an error that occurred in the application layer. Designed to be easily mapped to HTTP
 * responses and provide structured error information.
 *
 * @param code A stable machine-readable error code owned by the receiving context
 * @param message A human-readable error message
 * @param details Optional additional context about the error
 */
@NullMarked
public record ApplicationError(String code, String message, @Nullable String details) {
  /** Requires an inspectable public code and message; diagnostic details remain optional. */
  public ApplicationError {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(message, "message");
    if (code.isBlank() || message.isBlank()) {
      throw new IllegalArgumentException("application error code and message must not be blank");
    }
  }

  /** Creates an ApplicationError with code and message only. */
  public ApplicationError(String code, String message) {
    this(code, message, null);
  }

  /** Validation error: input data is invalid or violates constraints */
  public static ApplicationError validationError(String fieldOrConcept, String reason) {
    return new ApplicationError(
        "VALIDATION_ERROR", "Validation failed: %s".formatted(fieldOrConcept), reason);
  }

  /** Not found error: the requested resource does not exist */
  public static ApplicationError notFound(String resourceType, String identifier) {
    return new ApplicationError(
        "%s_NOT_FOUND".formatted(resourceType.toUpperCase(Locale.ROOT)),
        "%s not found: %s".formatted(resourceType, identifier),
        null);
  }

  /** Business rule violation error: operation violates domain constraints */
  public static ApplicationError businessRuleViolation(String rule, String reason) {
    return new ApplicationError(
        "BUSINESS_RULE_VIOLATION", "Business rule violation: %s".formatted(rule), reason);
  }

  /** Conflict error: operation cannot be completed due to conflicting state */
  public static ApplicationError conflict(String resource, String reason) {
    return new ApplicationError(
        "%s_CONFLICT".formatted(resource.toUpperCase(Locale.ROOT)),
        "Conflict with %s".formatted(resource),
        reason);
  }

  /** Unexpected error: something went wrong that shouldn't have */
  public static ApplicationError unexpected(String context, String reason) {
    return new ApplicationError(
        "UNEXPECTED_ERROR", "Unexpected error in %s".formatted(context), reason);
  }
}
