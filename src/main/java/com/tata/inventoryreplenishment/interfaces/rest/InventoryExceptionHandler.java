package com.tata.inventoryreplenishment.interfaces.rest;

import com.tata.inventoryreplenishment.application.internal.InventoryApplicationException;
import com.tata.inventoryreplenishment.interfaces.rest.resources.ErrorResource;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

/** Turns Inventory failures into stable error codes with messages localized by Accept-Language. */
@RestControllerAdvice(assignableTypes = InventoryController.class)
public class InventoryExceptionHandler {
    private final MessageSource messageSource;

    public InventoryExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(InventoryApplicationException.class)
    public ResponseEntity<ErrorResource> handleInventory(InventoryApplicationException exception, Locale locale) {
        var status = switch (exception.code()) {
            case INVENTORY_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVENTORY_ALREADY_EXISTS, INSUFFICIENT_STOCK -> HttpStatus.CONFLICT;
            case INVALID_QUANTITY -> HttpStatus.BAD_REQUEST;
        };
        return error(status, exception.code().name(), locale);
    }

    /** Two concurrent registrations for the same medication pass the existence check; the unique key rejects one. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResource> handleDuplicate(DataIntegrityViolationException exception, Locale locale) {
        return error(HttpStatus.CONFLICT, InventoryApplicationException.Code.INVENTORY_ALREADY_EXISTS.name(), locale);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResource> handleConcurrentUpdate(OptimisticLockingFailureException exception, Locale locale) {
        return error(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", locale);
    }

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
