package com.vitahealth.tata.inventoryreplenishment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Caregiver request for one receipt; stock quantity rules are enforced by the aggregate. */
@Schema(description = "Units added to an existing inventory (US-43)")
public record RegisterReplenishmentResource(
    @Schema(description = "Units added; must be positive", example = "30") @NotNull
        Integer quantity,
    @Schema(description = "Optional lot identifier or note", example = "LOT-2026-10")
        @Size(max = 200)
        String lot) {
  /** Accepts requests without an optional lot reference. */
  public RegisterReplenishmentResource(Integer quantity) {
    this(quantity, null);
  }
}
