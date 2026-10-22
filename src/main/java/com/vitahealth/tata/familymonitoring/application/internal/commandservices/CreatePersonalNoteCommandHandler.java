package com.vitahealth.tata.familymonitoring.application.internal.commandservices;

import com.vitahealth.tata.familymonitoring.application.commandservices.PersonalNoteCommandService;
import com.vitahealth.tata.familymonitoring.domain.model.commands.CreatePersonalNoteCommand;
import com.vitahealth.tata.familymonitoring.domain.model.entities.PersonalNote;
import com.vitahealth.tata.familymonitoring.domain.repositories.PersonalNoteRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Appends private adult reminders using the application clock. */
@Service
public class CreatePersonalNoteCommandHandler implements PersonalNoteCommandService {
  private final PersonalNoteRepository repository;
  private final Clock clock;

  /** Injects append-only note storage and the recording clock. */
  public CreatePersonalNoteCommandHandler(PersonalNoteRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /** Records a new private note and returns its assigned stored identity. */
  @Override
  @Transactional
  public PersonalNote handle(CreatePersonalNoteCommand command) {
    return repository.append(
        new PersonalNote(
            null,
            command.olderAdultId(),
            command.title(),
            command.text(),
            command.category(),
            clock.instant()));
  }
}
