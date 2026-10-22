package com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform;

import com.vitahealth.tata.treatmentmanagement.domain.model.commands.RegisterMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources.RegisterMedicationRequest;

/** Maps RegisterMedicationRequest and route identity to an explicit application command. */
public final class RegisterMedicationCommandFromResourceAssembler {
  private RegisterMedicationCommandFromResourceAssembler() {}

  /** Preserves caller data while taking the resource identity from the route. */
  public static RegisterMedicationCommand toCommand(
      String olderAdultId, RegisterMedicationRequest resource) {
    return new RegisterMedicationCommand(
        resource.caregiverId(), olderAdultId, resource.name(), resource.presentation());
  }
}
