package com.tata.familymonitoring.application.commandservices;

import com.tata.familymonitoring.domain.model.commands.CreateCaregiverNoteCommand;
import com.tata.familymonitoring.domain.model.entities.CaregiverNote;

public interface CreateCaregiverNoteCommandService {
    CaregiverNote handle(CreateCaregiverNoteCommand command);
}
