package com.vitahealth.tata.familymonitoring.application.commandservices;

import com.vitahealth.tata.familymonitoring.application.result.CaregiverNoteResult;
import com.vitahealth.tata.familymonitoring.domain.model.commands.CreateCaregiverNoteCommand;

/** Registers author-owned interventions and returns their persisted identity. */
public interface CreateCaregiverNoteCommandService {
  /** Returns a snapshot of the requested persisted evidence. */
  CaregiverNoteResult handle(CreateCaregiverNoteCommand command);
}
