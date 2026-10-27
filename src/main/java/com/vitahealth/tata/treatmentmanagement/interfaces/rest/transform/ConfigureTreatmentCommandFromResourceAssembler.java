package com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform;

import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ConfigureTreatmentCommand;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources.ConfigureTreatmentRequest;

/** Maps ConfigureTreatmentRequest and route identity to an explicit application command. */
public final class ConfigureTreatmentCommandFromResourceAssembler {
  private ConfigureTreatmentCommandFromResourceAssembler() {}

  /** Preserves caller data while taking the resource identity from the route. */
  public static ConfigureTreatmentCommand toCommand(
      String treatmentId, ConfigureTreatmentRequest resource) {
    return new ConfigureTreatmentCommand(
        resource.caregiverId(),
        treatmentId,
        resource.medicationId(),
        resource.dose(),
        resource.frequency(),
        resource.scheduledTimes(),
        resource.instructions(),
        resource.reminderLeadMinutes(),
        resource.recurrenceMode(),
        resource.recurrenceZone(),
        resource.recurrenceAnchor(),
        resource.followUpDelayMinutes());
  }
}
