package com.vitahealth.tata.inventoryreplenishment.interfaces.rest;

import com.vitahealth.tata.inventoryreplenishment.application.InventoryApplicationException;
import com.vitahealth.tata.inventoryreplenishment.application.exceptions.InvalidInventorySourceException;
import com.vitahealth.tata.inventoryreplenishment.interfaces.rest.resources.ErrorResource;
import com.vitahealth.tata.shared.infrastructure.persistence.jpa.exceptions.UniqueConstraintClassifier;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns Inventory failures into stable error codes with messages localized by Accept-Language. */
@RestControllerAdvice(assignableTypes = InventoryController.class)
public class InventoryExceptionHandler {
  private final MessageSource messageSource;

  /** Injects localized public error messages. */
  public InventoryExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /** Maps expected application failures while retaining their stable public codes. */
  @ExceptionHandler(InventoryApplicationException.class)
  public ResponseEntity<ErrorResource> handleInventory(
      InventoryApplicationException exception, Locale locale) {
    var status =
        switch (exception.code()) {
          case MEDICATION_NOT_FOUND, INVENTORY_NOT_FOUND -> HttpStatus.NOT_FOUND;
          case MEDICATION_INACTIVE, INVENTORY_ALREADY_EXISTS, INSUFFICIENT_STOCK ->
              HttpStatus.CONFLICT;
          case INVALID_QUANTITY -> HttpStatus.BAD_REQUEST;
        };
    return error(status, exception.code().name(), locale);
  }

  /** Recognizes only the inventory medication uniqueness constraint as a registration conflict. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResource> handleDuplicate(
      DataIntegrityViolationException exception, Locale locale) {
    if (UniqueConstraintClassifier.matches(exception, "uk_inventories_medication")) {
      return error(
          HttpStatus.CONFLICT,
          InventoryApplicationException.Code.INVENTORY_ALREADY_EXISTS.name(),
          locale);
    }
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "INVENTORY_STORAGE_ERROR", locale);
  }

  /** Keeps malformed owning-context evidence distinct from invalid caregiver input. */
  @ExceptionHandler(InvalidInventorySourceException.class)
  public ResponseEntity<ErrorResource> handleInvalidSource(
      InvalidInventorySourceException exception, Locale locale) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "INVENTORY_SOURCE_ERROR", locale);
  }

  /** Reports a stale write without exposing provider diagnostics. */
  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<ErrorResource> handleConcurrentUpdate(
      OptimisticLockingFailureException exception, Locale locale) {
    return error(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", locale);
  }

  /** Reports malformed or invalid transport input using a localized safe envelope. */
  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class
  })
  public ResponseEntity<ErrorResource> handleValidation(Exception exception, Locale locale) {
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", locale);
  }

  private ResponseEntity<ErrorResource> error(HttpStatus status, String code, Locale locale) {
    var key = "inventory.error." + code.toLowerCase(Locale.ROOT).replace('_', '-');
    var message = messageSource.getMessage(key, null, code, locale);
    return ResponseEntity.status(status).body(new ErrorResource(code, message));
  }
}
