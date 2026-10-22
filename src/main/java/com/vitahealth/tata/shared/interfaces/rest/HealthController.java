package com.vitahealth.tata.shared.interfaces.rest;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes process liveness without claiming database or external-provider readiness. */
@RestController
public class HealthController {
  /** Returns the existing UP response for both supported liveness probe paths. */
  @GetMapping({"/health", "/actuator/health"})
  public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of("status", "UP"));
  }
}
