package com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform;

import com.vitahealth.tata.treatmentmanagement.domain.model.commands.CreateTreatmentCommand;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources.CreateTreatmentRequest;

/** Maps CreateTreatmentRequest and route identity to an explicit application command. */
public final class CreateTreatmentCommandFromResourceAssembler {
  private CreateTreatmentCommandFromResourceAssembler() {}

  /** Preserves caller data while taking the resource identity from the route. */
  public static CreateTreatmentCommand toCommand(
      String olderAdultId, CreateTreatmentRequest resource) {
    return new CreateTreatmentCommand(resource.caregiverId(), olderAdultId, resource.name());
  }
}
