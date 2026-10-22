package com.vitahealth.tata.inventoryreplenishment.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Verifies storage faults cannot masquerade as medication registration conflicts. */
class InventoryIntegrityErrorTest {
  private void response(String constraint, String sqlState, int expectedStatus, String code)
      throws Exception {
    var messages = new ResourceBundleMessageSource();
    messages.setBasename("i18n/messages");
    messages.setDefaultEncoding("UTF-8");
    var advice = new InventoryExceptionHandler(messages);
    var controller =
        new FailingController(
            new DataIntegrityViolationException(
                "private SQL details",
                new ConstraintViolationException(
                    "private provider details",
                    new SQLException("private", sqlState),
                    constraint)));
    MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(advice)
        .build()
        .perform(get("/api/v1/inventories/integrity").header("Accept-Language", "es"))
        .andExpect(status().is(expectedStatus))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(
            jsonPath("$.message")
                .value(
                    expectedStatus == 409
                        ? "Ya existe un inventario registrado para este medicamento"
                        : "No se pudo actualizar el inventario"));
  }

  @Test
  void exactUniqueRegistrationConstraintRemainsAConflict() throws Exception {
    response("uk_inventories_medication", "23505", 409, "INVENTORY_ALREADY_EXISTS");
  }

  @Test
  void unrelatedOrNonUniqueConstraintIsATechnicalFailure() throws Exception {
    response("receipt_unique", "23505", 500, "INVENTORY_STORAGE_ERROR");
    response("uk_inventories_medication", "23502", 500, "INVENTORY_STORAGE_ERROR");
    response(null, "23505", 500, "INVENTORY_STORAGE_ERROR");
  }

  /** Fixture exposes a controlled failure through the actual MVC exception resolution chain. */
  @RestController
  static class FailingController extends InventoryController {
    private final RuntimeException failure;

    FailingController(RuntimeException failure) {
      super(null, null);
      this.failure = failure;
    }

    @GetMapping("/integrity")
    public void fail() {
      throw failure;
    }
  }
}
