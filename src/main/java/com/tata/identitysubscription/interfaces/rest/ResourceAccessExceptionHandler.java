package com.tata.identitysubscription.interfaces.rest;
import com.tata.identitysubscription.application.models.ResourceAccessDenied;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;
import java.util.Map;
@RestControllerAdvice
public class ResourceAccessExceptionHandler {
 @ExceptionHandler(ResourceAccessDenied.class)
 public ResponseEntity<Map<String,String>> denied(ResourceAccessDenied ex) { return ResponseEntity.status(403).body(Map.of("code","RESOURCE_ACCESS_DENIED","message",ex.getMessage())); }
}
