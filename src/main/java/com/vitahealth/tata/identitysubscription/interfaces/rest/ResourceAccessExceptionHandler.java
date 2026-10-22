package com.vitahealth.tata.identitysubscription.interfaces.rest;

import com.vitahealth.tata.identitysubscription.application.exceptions.ResourceAccessDenied;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps ownership failures to a stable response that does not disclose private resource details. */
@RestControllerAdvice
public class ResourceAccessExceptionHandler {
  /** Returns the existing access-denied contract for authenticated requests without permission. */
  @ExceptionHandler(ResourceAccessDenied.class)
  public ResponseEntity<Map<String, String>> denied(ResourceAccessDenied ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(Map.of("code", "RESOURCE_ACCESS_DENIED", "message", ex.getMessage()));
  }
}
