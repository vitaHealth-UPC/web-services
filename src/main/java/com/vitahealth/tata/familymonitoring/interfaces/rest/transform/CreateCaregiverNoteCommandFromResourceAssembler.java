package com.vitahealth.tata.familymonitoring.interfaces.rest.transform;

import com.vitahealth.tata.familymonitoring.domain.model.commands.CreateCaregiverNoteCommand;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.CreateCaregiverNoteResource;

/** Maps intervention request content to the application command after controller access checks. */
public final class CreateCaregiverNoteCommandFromResourceAssembler {

  private CreateCaregiverNoteCommandFromResourceAssembler() {}

  /** Combines the adult path selector with the supplied author and intervention content. */
  public static CreateCaregiverNoteCommand toCommandFromResource(
      String olderAdultId, CreateCaregiverNoteResource resource) {
    return new CreateCaregiverNoteCommand(olderAdultId, resource.familiarId(), resource.text());
  }
}
