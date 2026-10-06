package com.tata.familymonitoring.interfaces.rest.transform;

import com.tata.familymonitoring.domain.model.commands.CreateCaregiverNoteCommand;
import com.tata.familymonitoring.interfaces.rest.resources.CreateCaregiverNoteResource;

public final class CreateCaregiverNoteCommandFromResourceAssembler {

  private CreateCaregiverNoteCommandFromResourceAssembler() {
  }

  public static CreateCaregiverNoteCommand toCommandFromResource(
      Long olderAdultId, CreateCaregiverNoteResource resource) {
    return new CreateCaregiverNoteCommand(olderAdultId, resource.familiarId(), resource.text());
  }
}
