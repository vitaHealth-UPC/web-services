package com.tata.familymonitoring.application.commandservices;

import com.tata.familymonitoring.domain.model.commands.CreatePersonalNoteCommand;
import com.tata.familymonitoring.domain.model.entities.PersonalNote;

public interface PersonalNoteCommandService {
  PersonalNote handle(CreatePersonalNoteCommand command);
}
