package com.vitahealth.tata.familymonitoring.application.commandservices;

import com.vitahealth.tata.familymonitoring.domain.model.commands.CreatePersonalNoteCommand;
import com.vitahealth.tata.familymonitoring.domain.model.entities.PersonalNote;

/** Creates private reminders separately from caregiver follow-up interventions. */
public interface PersonalNoteCommandService {
  /** Returns the newly appended immutable reminder. */
  PersonalNote handle(CreatePersonalNoteCommand command);
}
