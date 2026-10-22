package com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform;

import com.vitahealth.tata.treatmentmanagement.domain.model.commands.UpdateMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources.UpdateMedicationRequest;

/** Maps UpdateMedicationRequest and route identity to an explicit application command. */
public final class UpdateMedicationCommandFromResourceAssembler {
  private UpdateMedicationCommandFromResourceAssembler() {}

  /** Preserves caller data while taking the resource identity from the route. */
  public static UpdateMedicationCommand toCommand(
      String medicationId, UpdateMedicationRequest resource) {
    return new UpdateMedicationCommand(
        resource.caregiverId(), medicationId, resource.name(), resource.presentation());
  }
}
